package com.wasd.qlic.notifications.interfaces.rest

import com.wasd.qlic.notifications.application.PushNotificationService
import com.wasd.qlic.notifications.domain.model.DevicePlatform
import com.wasd.qlic.notifications.domain.model.DeviceToken
import com.wasd.qlic.notifications.domain.model.NotificationCategory
import com.wasd.qlic.notifications.domain.model.PushDelivery
import com.wasd.qlic.notifications.domain.model.PushDeliveryStatus
import com.wasd.qlic.notifications.domain.model.PushMessage
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@Schema(description = "Registration token that the mobile app obtains from Firebase Cloud Messaging (TS06).")
data class RegisterDeviceTokenResource(
    @field:NotBlank(message = "{validation.device-token.required}")
    @field:Size(max = 512, message = "{validation.device-token.size}")
    @field:Schema(example = "fcm-registration-token-of-the-device")
    val token: String?,
    @field:Schema(example = "ANDROID", defaultValue = "ANDROID")
    val platform: DevicePlatform? = DevicePlatform.ANDROID
)

@Schema(description = "Device registered to receive push notifications; only the end of the token is shown.")
data class DeviceTokenResource(
    val id: UUID,
    @field:Schema(example = "…a1b2c3d4") val maskedToken: String,
    val platform: DevicePlatform,
    @field:Schema(description = "False once the provider rejected the token (TS06 - Scenario 2)") val active: Boolean,
    val registeredAt: Instant,
    @field:Schema(nullable = true) val lastSentAt: Instant?,
    @field:Schema(nullable = true) val invalidatedAt: Instant?
)

@Schema(description = "Record of a delivery attempt to the push provider (TS06 - Scenario 1).")
data class PushDeliveryResource(
    val id: UUID,
    val category: NotificationCategory,
    val title: String,
    @field:Schema(example = "…a1b2c3d4") val maskedToken: String,
    val status: PushDeliveryStatus,
    @field:Schema(nullable = true) val providerMessageId: String?,
    @field:Schema(nullable = true, example = "UNREGISTERED") val errorCode: String?,
    val attemptedAt: Instant
)

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Push Notifications", description = "Device tokens and push deliveries through Firebase Cloud Messaging (TS06, SP02)")
class NotificationController(
    private val pushService: PushNotificationService,
    private val messageSource: MessageSource
) {

    @PostMapping("/device-tokens")
    @Operation(
        summary = "Register the device of the app to receive push notifications (TS06)",
        description = "Call it after sign-in and whenever Firebase renews the token. Registering an existing token moves it to " +
            "this account and reactivates it."
    )
    fun registerToken(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: RegisterDeviceTokenResource
    ): ResponseEntity<DeviceTokenResource> {
        val token = pushService.registerToken(
            jwt.accountId(), requireNotNull(resource.token), resource.platform ?: DevicePlatform.ANDROID
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(token.toResource())
    }

    @GetMapping("/device-tokens")
    @Operation(summary = "List the devices of the account registered for push notifications")
    fun tokens(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<DeviceTokenResource>> =
        ResponseEntity.ok(pushService.tokens(jwt.accountId()).map { it.toResource() })

    @DeleteMapping("/device-tokens")
    @Operation(summary = "Stop sending push notifications to a device (for example, on sign-out)")
    fun unregisterToken(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam token: String
    ): ResponseEntity<Void> {
        pushService.unregisterToken(jwt.accountId(), token)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/test")
    @Operation(
        summary = "Send a test push notification to the devices of the account",
        description = "Useful to validate the integration with Firebase from the app; the result of each attempt is returned."
    )
    fun test(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<PushDeliveryResource>> {
        val locale = LocaleContextHolder.getLocale()
        val message = PushMessage(
            title = messageSource.getMessage("push.test.title", null, locale),
            body = messageSource.getMessage("push.test.body", null, locale),
            category = NotificationCategory.TEST
        )
        return ResponseEntity.ok(pushService.sendToAccount(jwt.accountId(), message).map { it.toResource() })
    }

    @GetMapping("/deliveries")
    @Operation(summary = "Latest push delivery attempts of the account (TS06 - Scenario 1)")
    fun deliveries(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<PushDeliveryResource>> =
        ResponseEntity.ok(pushService.deliveries(jwt.accountId()).map { it.toResource() })

    private fun DeviceToken.toResource() =
        DeviceTokenResource(id, maskedToken, platform, active, registeredAt, lastSentAt, invalidatedAt)

    private fun PushDelivery.toResource() =
        PushDeliveryResource(id, category, title, maskedToken, status, providerMessageId, errorCode, attemptedAt)
}
