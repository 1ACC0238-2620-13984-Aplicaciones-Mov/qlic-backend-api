package com.wasd.qlic.alerting.interfaces.rest

import com.wasd.qlic.alerting.application.commands.*
import com.wasd.qlic.alerting.application.handlers.AlertManagementCommandHandler
import com.wasd.qlic.alerting.application.handlers.EvaluateConsumptionReadingCommandHandler
import com.wasd.qlic.alerting.application.handlers.GetAlertsByAccountQueryHandler
import com.wasd.qlic.alerting.application.queries.GetAlertsByAccountQuery
import com.wasd.qlic.alerting.domain.exceptions.AlertNotFoundException
import com.wasd.qlic.alerting.domain.model.AuthorizedAlertRecipient
import com.wasd.qlic.alerting.domain.model.ConsumptionThreshold
import com.wasd.qlic.alerting.domain.model.LeakAlert
import com.wasd.qlic.alerting.domain.model.LeakAlertRaisedEvent
import com.wasd.qlic.alerting.domain.model.NotificationPreference
import com.wasd.qlic.alerting.interfaces.rest.resources.*
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

/** Alerting endpoints. Every operation works on the account of the access token. */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Leak alerts, thresholds, notification preferences and recipients")
class LeakAlertController(
    private val evaluateHandler: EvaluateConsumptionReadingCommandHandler,
    private val getAlertsHandler: GetAlertsByAccountQueryHandler,
    private val managementHandler: AlertManagementCommandHandler,
    private val eventPublisher: ApplicationEventPublisher
) {

    @PostMapping("/evaluations")
    @Operation(summary = "Evaluate a consumption reading and raise a leak alert when needed (US15, US17-US19)")
    fun evaluateReading(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: EvaluateReadingRequest
    ): ResponseEntity<LeakAlertResource> {
        val accountId = jwt.accountId()
        val effectiveLimit = request.thresholdLimit ?: managementHandler
            .getThresholdsByAccount(accountId)
            .find { it.waterPointId == request.waterPointId }
            ?.effectiveLimit()
        ?: DEFAULT_LIMIT_LITERS_PER_HOUR

        val result = evaluateHandler.handle(
            EvaluateConsumptionReadingCommand(
                accountId = accountId,
                waterPointId = request.waterPointId,
                currentFlowLitersPerHour = request.currentFlowLitersPerHour,
                thresholdLimit = effectiveLimit,
                isSustained = request.isSustained
            )
        ) ?: return ResponseEntity.noContent().build()

        val pushDispatched = managementHandler.getNotificationPreference(accountId).shouldSendPush(result.urgency)
        val notifiedUsers = if (pushDispatched) managementHandler.dispatchPushToAuthorizedUsers(accountId) else emptyList()
        if (pushDispatched) {
            eventPublisher.publishEvent(
                LeakAlertRaisedEvent(
                    accountId = accountId,
                    alertId = result.id,
                    waterPointId = result.waterPointId,
                    urgency = result.urgency.name,
                    estimatedVolumeLiters = result.estimatedVolumeLiters,
                    recipientPushTokens = notifiedUsers.map { it.devicePushToken }
                )
            )
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(result.toResource(pushDispatched = pushDispatched, notifiedRecipientsCount = notifiedUsers.size))
    }

    @GetMapping
    @Operation(summary = "List the alerts of the account (US16)")
    fun getAlertsByAccount(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<LeakAlertResource>> =
        ResponseEntity.ok(getAlertsHandler.handle(GetAlertsByAccountQuery(jwt.accountId())).map { it.toResource() })

    @GetMapping("/{alertId}")
    @Operation(summary = "Get the detail of an alert of the account (US16)")
    fun getAlertDetail(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable alertId: UUID
    ): ResponseEntity<LeakAlertResource> {
        val alert = managementHandler.getAlertDetail(alertId, jwt.accountId()) ?: throw AlertNotFoundException()
        return ResponseEntity.ok(alert.toResource())
    }

    @PutMapping("/thresholds")
    @Operation(summary = "Configure the consumption threshold of a Water Point (US17)")
    fun configureThreshold(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: ConfigureThresholdRequest
    ): ResponseEntity<ConsumptionThresholdResource> {
        val saved = managementHandler.configureThreshold(
            ConfigureThresholdCommand(
                accountId = jwt.accountId(),
                waterPointId = req.waterPointId,
                period = req.period,
                customLimitLitersPerHour = req.customLimitLitersPerHour,
                baselineLitersPerHour = req.baselineLitersPerHour
            )
        )
        return ResponseEntity.ok(saved.toResource())
    }

    @GetMapping("/thresholds")
    @Operation(summary = "List the consumption thresholds of the account (US17)")
    fun getThresholds(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<ConsumptionThresholdResource>> =
        ResponseEntity.ok(managementHandler.getThresholdsByAccount(jwt.accountId()).map { it.toResource() })

    @PutMapping("/preferences")
    @Operation(summary = "Configure the notification frequency and minimum urgency (US18)")
    fun configurePreferences(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: ConfigureNotificationPreferenceRequest
    ): ResponseEntity<NotificationPreferenceResource> {
        val saved = managementHandler.configureNotificationPreference(
            ConfigureNotificationPreferenceCommand(
                accountId = jwt.accountId(),
                groupingWindowMinutes = req.groupingWindowMinutes,
                minimumUrgency = req.minimumUrgency
            )
        )
        return ResponseEntity.ok(saved.toResource())
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get the notification preferences of the account (US18)")
    fun getPreferences(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<NotificationPreferenceResource> =
        ResponseEntity.ok(managementHandler.getNotificationPreference(jwt.accountId()).toResource())

    @PostMapping("/recipients")
    @Operation(summary = "Add a user who receives the alerts of the account (US19)")
    fun addAuthorizedRecipient(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: AddAuthorizedRecipientRequest
    ): ResponseEntity<AuthorizedRecipientResource> {
        val saved = managementHandler.addAuthorizedRecipient(
            AddAuthorizedRecipientCommand(
                accountId = jwt.accountId(),
                fullName = req.fullName,
                email = req.email,
                devicePushToken = req.devicePushToken,
                enabled = req.enabled
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResource())
    }

    @GetMapping("/recipients")
    @Operation(summary = "List the users who receive the alerts of the account (US19)")
    fun getAuthorizedRecipients(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<AuthorizedRecipientResource>> =
        ResponseEntity.ok(managementHandler.getRecipientsByAccount(jwt.accountId()).map { it.toResource() })

    @PatchMapping("/recipients/{recipientId}/toggle")
    @Operation(summary = "Enable or disable an alert recipient of the account (US19)")
    fun toggleAuthorizedRecipient(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recipientId: UUID,
        @RequestBody req: ToggleAuthorizedRecipientRequest
    ): ResponseEntity<AuthorizedRecipientResource> =
        ResponseEntity.ok(managementHandler.toggleRecipient(recipientId, jwt.accountId(), req.enabled).toResource())

    @PatchMapping("/{alertId}/acknowledge")
    @Operation(summary = "Confirm that an alert of the account was attended (US20)")
    fun acknowledgeAlert(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable alertId: UUID,
        @RequestBody req: AcknowledgeAlertRequest
    ): ResponseEntity<LeakAlertResource> {
        val updated = managementHandler.acknowledgeAlert(
            AcknowledgeAlertCommand(alertId = alertId, accountId = jwt.accountId(), actionTaken = req.actionTaken)
        )
        return ResponseEntity.ok(updated.toResource())
    }

    private fun LeakAlert.toResource(pushDispatched: Boolean = true, notifiedRecipientsCount: Int = 0) =
        LeakAlertResource(
            id = id,
            accountId = accountId,
            waterPointId = waterPointId,
            state = state,
            urgency = urgency,
            detectedAt = detectedAt,
            estimatedVolumeLiters = estimatedVolumeLiters,
            estimatedCostSoles = estimatedCostSoles,
            recommendedAction = recommendedAction,
            acknowledgedAction = acknowledgedAction,
            previousAlertId = previousAlertId,
            pushDispatched = pushDispatched,
            notifiedRecipientsCount = notifiedRecipientsCount
        )

    private fun ConsumptionThreshold.toResource() = ConsumptionThresholdResource(
        id = id,
        accountId = accountId,
        waterPointId = waterPointId,
        period = period,
        customLimitLitersPerHour = customLimitLitersPerHour,
        baselineLitersPerHour = baselineLitersPerHour,
        effectiveLimitLitersPerHour = effectiveLimit(),
        source = source,
        updatedAt = updatedAt
    )

    private fun NotificationPreference.toResource() = NotificationPreferenceResource(
        id = id,
        accountId = accountId,
        groupingWindowMinutes = groupingWindowMinutes,
        minimumUrgency = minimumUrgency,
        updatedAt = updatedAt
    )

    private fun AuthorizedAlertRecipient.toResource() = AuthorizedRecipientResource(
        id = id,
        accountId = accountId,
        fullName = fullName,
        email = email,
        devicePushToken = devicePushToken,
        enabled = enabled,
        lastNotifiedAt = lastNotifiedAt
    )

    private companion object {
        val DEFAULT_LIMIT_LITERS_PER_HOUR = BigDecimal("125.00")
    }
}
