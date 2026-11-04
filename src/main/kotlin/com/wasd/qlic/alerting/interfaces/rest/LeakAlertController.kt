package com.wasd.qlic.alerting.interfaces.rest

import com.wasd.qlic.alerting.application.commands.*
import com.wasd.qlic.alerting.application.handlers.AlertManagementCommandHandler
import com.wasd.qlic.alerting.application.handlers.EvaluateConsumptionReadingCommandHandler
import com.wasd.qlic.alerting.application.handlers.GetAlertsByAccountQueryHandler
import com.wasd.qlic.alerting.application.queries.GetAlertsByAccountQuery
import com.wasd.qlic.alerting.interfaces.rest.resources.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/alerts")
class LeakAlertController(
    private val evaluateHandler: EvaluateConsumptionReadingCommandHandler,
    private val getAlertsHandler: GetAlertsByAccountQueryHandler,
    private val managementHandler: AlertManagementCommandHandler
) {

    // US15: Ingesta y evaluación de lecturas de consumo (integrado con US17, US18, US19 y US20)
    @PostMapping("/evaluations")
    fun evaluateReading(@RequestBody request: EvaluateReadingRequest): ResponseEntity<Any> {
        val effectiveLimit = request.thresholdLimit ?: managementHandler
            .getThresholdsByAccount(request.accountId)
            .find { it.waterPointId == request.waterPointId }
            ?.effectiveLimit()
        ?: BigDecimal("125.00")

        val command = EvaluateConsumptionReadingCommand(
            accountId = request.accountId,
            waterPointId = request.waterPointId,
            currentFlowLitersPerHour = request.currentFlowLitersPerHour,
            thresholdLimit = effectiveLimit,
            isSustained = request.isSustained
        )

        val result = evaluateHandler.handle(command)
        return if (result != null) {
            val preference = managementHandler.getNotificationPreference(request.accountId)
            val pushDispatched = preference.shouldSendPush(result.urgency)
            val notifiedUsers = if (pushDispatched) {
                managementHandler.dispatchPushToAuthorizedUsers(request.accountId)
            } else {
                emptyList()
            }

            val resource = LeakAlertResource(
                id = result.id,
                accountId = result.accountId,
                waterPointId = result.waterPointId,
                state = result.state,
                urgency = result.urgency,
                detectedAt = result.detectedAt,
                estimatedVolumeLiters = result.estimatedVolumeLiters,
                estimatedCostSoles = result.estimatedCostSoles,
                recommendedAction = result.recommendedAction,
                acknowledgedAction = result.acknowledgedAction,
                previousAlertId = result.previousAlertId,
                pushDispatched = pushDispatched,
                notifiedRecipientsCount = notifiedUsers.size
            )
            ResponseEntity.status(HttpStatus.CREATED).body(resource)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    // US16: Consulta de alertas por cuenta
    @GetMapping
    fun getAlertsByAccount(@RequestParam accountId: UUID): ResponseEntity<List<LeakAlertResource>> {
        val query = GetAlertsByAccountQuery(accountId)
        val alerts = getAlertsHandler.handle(query).map {
            LeakAlertResource(
                id = it.id,
                accountId = it.accountId,
                waterPointId = it.waterPointId,
                state = it.state,
                urgency = it.urgency,
                detectedAt = it.detectedAt,
                estimatedVolumeLiters = it.estimatedVolumeLiters,
                estimatedCostSoles = it.estimatedCostSoles,
                recommendedAction = it.recommendedAction,
                acknowledgedAction = it.acknowledgedAction,
                previousAlertId = it.previousAlertId
            )
        }
        return ResponseEntity.ok(alerts)
    }

    // US16 (Alert Detail): Consulta del detalle de una alerta específica
    @GetMapping("/{alertId}")
    fun getAlertDetail(
        @PathVariable alertId: UUID,
        @RequestParam accountId: UUID
    ): ResponseEntity<LeakAlertResource> {
        val alert = managementHandler.getAlertDetail(alertId, accountId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            LeakAlertResource(
                id = alert.id,
                accountId = alert.accountId,
                waterPointId = alert.waterPointId,
                state = alert.state,
                urgency = alert.urgency,
                detectedAt = alert.detectedAt,
                estimatedVolumeLiters = alert.estimatedVolumeLiters,
                estimatedCostSoles = alert.estimatedCostSoles,
                recommendedAction = alert.recommendedAction,
                acknowledgedAction = alert.acknowledgedAction,
                previousAlertId = alert.previousAlertId
            )
        )
    }

    // US17: Configurar umbral de consumo (propio o automático)
    @PutMapping("/thresholds")
    fun configureThreshold(@RequestBody req: ConfigureThresholdRequest): ResponseEntity<ConsumptionThresholdResource> {
        val saved = managementHandler.configureThreshold(
            ConfigureThresholdCommand(
                accountId = req.accountId,
                waterPointId = req.waterPointId,
                period = req.period,
                customLimitLitersPerHour = req.customLimitLitersPerHour,
                baselineLitersPerHour = req.baselineLitersPerHour
            )
        )
        return ResponseEntity.ok(
            ConsumptionThresholdResource(
                id = saved.id,
                accountId = saved.accountId,
                waterPointId = saved.waterPointId,
                period = saved.period,
                customLimitLitersPerHour = saved.customLimitLitersPerHour,
                baselineLitersPerHour = saved.baselineLitersPerHour,
                effectiveLimitLitersPerHour = saved.effectiveLimit(),
                source = saved.source,
                updatedAt = saved.updatedAt
            )
        )
    }

    @GetMapping("/thresholds")
    fun getThresholds(@RequestParam accountId: UUID): ResponseEntity<List<ConsumptionThresholdResource>> {
        val list = managementHandler.getThresholdsByAccount(accountId).map {
            ConsumptionThresholdResource(
                id = it.id,
                accountId = it.accountId,
                waterPointId = it.waterPointId,
                period = it.period,
                customLimitLitersPerHour = it.customLimitLitersPerHour,
                baselineLitersPerHour = it.baselineLitersPerHour,
                effectiveLimitLitersPerHour = it.effectiveLimit(),
                source = it.source,
                updatedAt = it.updatedAt
            )
        }
        return ResponseEntity.ok(list)
    }

    // US18: Configurar preferencias y frecuencia de notificaciones
    @PutMapping("/preferences")
    fun configurePreferences(
        @RequestBody req: ConfigureNotificationPreferenceRequest
    ): ResponseEntity<NotificationPreferenceResource> {
        val saved = managementHandler.configureNotificationPreference(
            ConfigureNotificationPreferenceCommand(
                accountId = req.accountId,
                groupingWindowMinutes = req.groupingWindowMinutes,
                minimumUrgency = req.minimumUrgency
            )
        )
        return ResponseEntity.ok(
            NotificationPreferenceResource(
                id = saved.id,
                accountId = saved.accountId,
                groupingWindowMinutes = saved.groupingWindowMinutes,
                minimumUrgency = saved.minimumUrgency,
                updatedAt = saved.updatedAt
            )
        )
    }

    @GetMapping("/preferences")
    fun getPreferences(@RequestParam accountId: UUID): ResponseEntity<NotificationPreferenceResource> {
        val pref = managementHandler.getNotificationPreference(accountId)
        return ResponseEntity.ok(
            NotificationPreferenceResource(
                id = pref.id,
                accountId = pref.accountId,
                groupingWindowMinutes = pref.groupingWindowMinutes,
                minimumUrgency = pref.minimumUrgency,
                updatedAt = pref.updatedAt
            )
        )
    }

    // US19: Registrar, consultar y habilitar/deshabilitar usuarios autorizados para alertas
    @PostMapping("/recipients")
    fun addAuthorizedRecipient(
        @RequestBody req: AddAuthorizedRecipientRequest
    ): ResponseEntity<AuthorizedRecipientResource> {
        val saved = managementHandler.addAuthorizedRecipient(
            AddAuthorizedRecipientCommand(
                accountId = req.accountId,
                fullName = req.fullName,
                email = req.email,
                devicePushToken = req.devicePushToken,
                enabled = req.enabled
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AuthorizedRecipientResource(
                id = saved.id,
                accountId = saved.accountId,
                fullName = saved.fullName,
                email = saved.email,
                devicePushToken = saved.devicePushToken,
                enabled = saved.enabled,
                lastNotifiedAt = saved.lastNotifiedAt
            )
        )
    }

    @GetMapping("/recipients")
    fun getAuthorizedRecipients(@RequestParam accountId: UUID): ResponseEntity<List<AuthorizedRecipientResource>> {
        val list = managementHandler.getRecipientsByAccount(accountId).map {
            AuthorizedRecipientResource(
                id = it.id,
                accountId = it.accountId,
                fullName = it.fullName,
                email = it.email,
                devicePushToken = it.devicePushToken,
                enabled = it.enabled,
                lastNotifiedAt = it.lastNotifiedAt
            )
        }
        return ResponseEntity.ok(list)
    }

    @PatchMapping("/recipients/{recipientId}/toggle")
    fun toggleAuthorizedRecipient(
        @PathVariable recipientId: UUID,
        @RequestBody req: ToggleAuthorizedRecipientRequest
    ): ResponseEntity<AuthorizedRecipientResource> {
        val updated = managementHandler.toggleRecipient(recipientId, req.enabled)
        return ResponseEntity.ok(
            AuthorizedRecipientResource(
                id = updated.id,
                accountId = updated.accountId,
                fullName = updated.fullName,
                email = updated.email,
                devicePushToken = updated.devicePushToken,
                enabled = updated.enabled,
                lastNotifiedAt = updated.lastNotifiedAt
            )
        )
    }

    // US20: Confirmar la atención de una alerta activa
    @PatchMapping("/{alertId}/acknowledge")
    fun acknowledgeAlert(
        @PathVariable alertId: UUID,
        @RequestBody req: AcknowledgeAlertRequest
    ): ResponseEntity<LeakAlertResource> {
        val updated = managementHandler.acknowledgeAlert(
            AcknowledgeAlertCommand(
                alertId = alertId,
                accountId = req.accountId,
                actionTaken = req.actionTaken
            )
        )
        return ResponseEntity.ok(
            LeakAlertResource(
                id = updated.id,
                accountId = updated.accountId,
                waterPointId = updated.waterPointId,
                state = updated.state,
                urgency = updated.urgency,
                detectedAt = updated.detectedAt,
                estimatedVolumeLiters = updated.estimatedVolumeLiters,
                estimatedCostSoles = updated.estimatedCostSoles,
                recommendedAction = updated.recommendedAction,
                acknowledgedAction = updated.acknowledgedAction,
                previousAlertId = updated.previousAlertId
            )
        )
    }
}