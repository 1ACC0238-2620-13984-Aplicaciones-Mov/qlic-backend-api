package com.wasd.qlic.alerting.interfaces.rest

import com.wasd.qlic.alerting.application.commands.*
import com.wasd.qlic.alerting.application.handlers.AlertManagementCommandHandler
import com.wasd.qlic.alerting.application.handlers.GetAlertsByAccountQueryHandler
import com.wasd.qlic.alerting.application.queries.GetAlertsByAccountQuery
import com.wasd.qlic.alerting.application.services.LeakDetectionService
import com.wasd.qlic.alerting.domain.exceptions.AlertNotFoundException
import com.wasd.qlic.alerting.domain.model.AuthorizedAlertRecipient
import com.wasd.qlic.alerting.domain.model.ConsumptionThreshold
import com.wasd.qlic.alerting.domain.model.LeakAlert
import com.wasd.qlic.alerting.domain.model.NotificationPreference
import com.wasd.qlic.alerting.interfaces.rest.resources.*
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** Alerting endpoints. Every operation works on the account of the access token. */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Leak alerts, thresholds, notification preferences and recipients")
class LeakAlertController(
    private val leakDetectionService: LeakDetectionService,
    private val getAlertsHandler: GetAlertsByAccountQueryHandler,
    private val managementHandler: AlertManagementCommandHandler
) {

    @PostMapping("/evaluations")
    @Operation(
        summary = "Evaluate a consumption reading and raise a leak alert when needed (US15, US17-US19)",
        description = "Compares the flow of a Water Point with its threshold: the one sent in thresholdLimit, else the " +
            "configured one, else the default. Only a sustained flow above the limit raises an alert (201 with " +
            "the alert); otherwise it answers 204 without body. When the urgency reaches the minimum configured, " +
            "the alert is notified to the enabled recipients and by push."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "A leak alert was raised or escalated"),
        ApiResponse(responseCode = "204", description = "The flow is within the threshold or not sustained; no alert", content = [Content()])
    )
    fun evaluateReading(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: EvaluateReadingRequest
    ): ResponseEntity<LeakAlertResource> {
        val outcome = leakDetectionService.evaluate(
            accountId = jwt.accountId(),
            waterPointId = request.waterPointId,
            flowLitersPerHour = request.currentFlowLitersPerHour,
            sustained = request.isSustained,
            thresholdLimit = request.thresholdLimit
        ) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.status(HttpStatus.CREATED).body(
            outcome.alert.toResource(
                pushDispatched = outcome.pushDispatched,
                notifiedRecipientsCount = outcome.notifiedRecipientsCount
            )
        )
    }

    @GetMapping
    @Operation(
        summary = "List the alerts of the account (US16)",
        description = "Returns the leak alerts of the account with their state, urgency, estimated volume and cost, and the " +
            "recommended action."
    )
    fun getAlertsByAccount(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<LeakAlertResource>> =
        ResponseEntity.ok(getAlertsHandler.handle(GetAlertsByAccountQuery(jwt.accountId())).map { it.toResource() })

    @GetMapping("/{alertId}")
    @Operation(
        summary = "Get the detail of an alert of the account (US16)",
        description = "Returns one alert with the estimated volume and cost, the recommended action and, when it was " +
            "attended, the action taken. An alert of another account returns 404."
    )
    fun getAlertDetail(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable alertId: UUID
    ): ResponseEntity<LeakAlertResource> {
        val alert = managementHandler.getAlertDetail(alertId, jwt.accountId()) ?: throw AlertNotFoundException()
        return ResponseEntity.ok(alert.toResource())
    }

    @PutMapping("/thresholds")
    @Operation(
        summary = "Configure the consumption threshold of a Water Point (US17)",
        description = "Sets the limit in liters per hour for a Water Point. With customLimitLitersPerHour the subscriber's " +
            "own limit applies (USER_DEFINED); without it, the limit is the baseline plus 25% " +
            "(SYSTEM_CALCULATED)."
    )
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
    @Operation(
        summary = "List the consumption thresholds of the account (US17)",
        description = "Returns the threshold of each configured Water Point with its effective limit and whether it was " +
            "defined by the user or calculated by the system."
    )
    fun getThresholds(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<ConsumptionThresholdResource>> =
        ResponseEntity.ok(managementHandler.getThresholdsByAccount(jwt.accountId()).map { it.toResource() })

    @PutMapping("/preferences")
    @Operation(
        summary = "Configure the notification frequency and minimum urgency (US18)",
        description = "Sets the minutes during which repeated alerts are grouped into one notification and the minimum " +
            "urgency that sends a push notification."
    )
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
    @Operation(
        summary = "Get the notification preferences of the account (US18)",
        description = "Returns the grouping window in minutes and the minimum urgency that sends a push notification. " +
            "Accounts without preferences get the defaults."
    )
    fun getPreferences(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<NotificationPreferenceResource> =
        ResponseEntity.ok(managementHandler.getNotificationPreference(jwt.accountId()).toResource())

    @PostMapping("/recipients")
    @Operation(
        summary = "Add a user who receives the alerts of the account (US19)",
        description = "Registers a person, with name, e-mail and push token of their device, who also receives the alerts " +
            "of the account."
    )
    @ResponseStatus(HttpStatus.CREATED)
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
    @Operation(
        summary = "List the users who receive the alerts of the account (US19)",
        description = "Returns the alert recipients of the account, whether they are enabled and when they were last " +
            "notified."
    )
    fun getAuthorizedRecipients(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<AuthorizedRecipientResource>> =
        ResponseEntity.ok(managementHandler.getRecipientsByAccount(jwt.accountId()).map { it.toResource() })

    @PatchMapping("/recipients/{recipientId}/toggle")
    @Operation(
        summary = "Enable or disable an alert recipient of the account (US19)",
        description = "Turns on or off the alerts for one recipient without deleting it. A recipient of another account " +
            "returns 404."
    )
    fun toggleAuthorizedRecipient(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recipientId: UUID,
        @RequestBody req: ToggleAuthorizedRecipientRequest
    ): ResponseEntity<AuthorizedRecipientResource> =
        ResponseEntity.ok(managementHandler.toggleRecipient(recipientId, jwt.accountId(), req.enabled).toResource())

    @PatchMapping("/{alertId}/acknowledge")
    @Operation(
        summary = "Confirm that an alert of the account was attended (US20)",
        description = "Registers the action taken by the subscriber and marks the alert as ACKNOWLEDGED. An alert of " +
            "another account returns 404."
    )
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
}
