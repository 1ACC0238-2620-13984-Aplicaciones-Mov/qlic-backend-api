package com.wasd.qlic.alerting.application.services

import com.wasd.qlic.alerting.application.commands.EvaluateConsumptionReadingCommand
import com.wasd.qlic.alerting.application.handlers.AlertManagementCommandHandler
import com.wasd.qlic.alerting.application.handlers.EvaluateConsumptionReadingCommandHandler
import com.wasd.qlic.alerting.domain.model.LeakAlert
import com.wasd.qlic.alerting.domain.model.LeakAlertRaisedEvent
import com.wasd.qlic.alerting.domain.repositories.LeakAlertRepository
import com.wasd.qlic.alerting.domain.services.AnomalyDetectionService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

/** Result of an evaluation that raised or escalated an alert. */
data class LeakEvaluationOutcome(
    val alert: LeakAlert,
    val pushDispatched: Boolean,
    val notifiedRecipientsCount: Int
)

/**
 * Application service of US15 and US17-US19: compares the flow of a Water Point with its effective threshold, raises
 * or escalates the leak alert and, when the urgency reaches the minimum of the notification preferences, publishes
 * LeakAlertRaisedEvent so the alert reaches the account and its enabled recipients by push.
 * It is used by the manual evaluation endpoint and by the automatic evaluation of every registered reading.
 */
@Service
class LeakDetectionService(
    private val evaluateHandler: EvaluateConsumptionReadingCommandHandler,
    private val managementHandler: AlertManagementCommandHandler,
    private val leakAlertRepository: LeakAlertRepository,
    private val anomalyDetectionService: AnomalyDetectionService,
    private val eventPublisher: ApplicationEventPublisher
) {

    /** Threshold that applies to the Water Point: the configured one or, without configuration, the default. */
    fun effectiveLimit(accountId: UUID, waterPointId: UUID): BigDecimal =
        managementHandler.getThresholdsByAccount(accountId)
            .find { it.waterPointId == waterPointId }
            ?.effectiveLimit()
            ?: DEFAULT_LIMIT_LITERS_PER_HOUR

    /**
     * Evaluates a flow. With notifyOnlyOnChange, an alert that is already active is only notified again when its
     * urgency rises, so a meter reporting every few minutes does not repeat the same push.
     */
    @Transactional
    fun evaluate(
        accountId: UUID,
        waterPointId: UUID,
        flowLitersPerHour: BigDecimal,
        sustained: Boolean,
        thresholdLimit: BigDecimal? = null,
        notifyOnlyOnChange: Boolean = false
    ): LeakEvaluationOutcome? = doEvaluate(
        accountId, waterPointId, flowLitersPerHour, sustained,
        thresholdLimit ?: effectiveLimit(accountId, waterPointId), notifyOnlyOnChange
    )

    /**
     * US15: automatic evaluation of a reading registered by a meter. It runs in its own transaction because it is
     * invoked after the transaction of the reading has committed. A flow within the threshold is ignored; above it,
     * the flow is sustained when the previous reading of the Water Point was also above it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun evaluateRegisteredReading(
        accountId: UUID,
        waterPointId: UUID,
        flowLitersPerHour: BigDecimal,
        previousFlowLitersPerHour: BigDecimal?
    ): LeakEvaluationOutcome? {
        val limit = effectiveLimit(accountId, waterPointId)
        if (flowLitersPerHour <= limit) return null
        val sustained = anomalyDetectionService.isSustained(previousFlowLitersPerHour, limit)
        return doEvaluate(accountId, waterPointId, flowLitersPerHour, sustained, limit, notifyOnlyOnChange = true)
    }

    private fun doEvaluate(
        accountId: UUID,
        waterPointId: UUID,
        flowLitersPerHour: BigDecimal,
        sustained: Boolean,
        limit: BigDecimal,
        notifyOnlyOnChange: Boolean
    ): LeakEvaluationOutcome? {
        val previousUrgency = leakAlertRepository.findActiveByWaterPointId(waterPointId)?.urgency

        val alert = evaluateHandler.handle(
            EvaluateConsumptionReadingCommand(
                accountId = accountId,
                waterPointId = waterPointId,
                currentFlowLitersPerHour = flowLitersPerHour,
                thresholdLimit = limit,
                isSustained = sustained
            )
        ) ?: return null

        val changed = previousUrgency == null || alert.urgency.ordinal > previousUrgency.ordinal
        val pushDispatched = (!notifyOnlyOnChange || changed) &&
            managementHandler.getNotificationPreference(accountId).shouldSendPush(alert.urgency)
        val notifiedUsers = if (pushDispatched) managementHandler.dispatchPushToAuthorizedUsers(accountId) else emptyList()
        if (pushDispatched) {
            eventPublisher.publishEvent(
                LeakAlertRaisedEvent(
                    accountId = accountId,
                    alertId = alert.id,
                    waterPointId = alert.waterPointId,
                    urgency = alert.urgency.name,
                    estimatedVolumeLiters = alert.estimatedVolumeLiters,
                    recipientPushTokens = notifiedUsers.map { it.devicePushToken }
                )
            )
        }
        return LeakEvaluationOutcome(alert, pushDispatched, notifiedUsers.size)
    }

    companion object {
        /** Limit used when the Water Point has no configured threshold (baseline 100 L/h plus 25%). */
        val DEFAULT_LIMIT_LITERS_PER_HOUR = BigDecimal("125.00")
    }
}
