package com.wasd.qlic.alerting.application.handlers

import com.wasd.qlic.alerting.application.services.LeakDetectionService
import com.wasd.qlic.monitoring.domain.model.ConsumptionReadingRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * US15: automatic leak detection. Every reading registered by a meter is evaluated against the threshold of its
 * Water Point once the reading is stored; an isolated peak never raises an alert. It runs after the reading commits
 * and the evaluation uses its own transaction, so a failure here is logged and never loses or rejects the reading.
 */
@Component
class ConsumptionReadingRegisteredEventHandler(
    private val leakDetectionService: LeakDetectionService
) {
    private val log = LoggerFactory.getLogger(ConsumptionReadingRegisteredEventHandler::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun on(event: ConsumptionReadingRegisteredEvent) {
        val flow = event.flowLitersPerHour ?: return
        try {
            leakDetectionService.evaluateRegisteredReading(
                accountId = event.accountId,
                waterPointId = event.waterPointId,
                flowLitersPerHour = flow,
                previousFlowLitersPerHour = event.previousFlowLitersPerHour
            )
        } catch (exception: RuntimeException) {
            log.error("The reading {} could not be evaluated for leaks", event.readingId, exception)
        }
    }
}
