package com.wasd.qlic.notifications.application

import com.wasd.qlic.alerting.domain.model.LeakAlertRaisedEvent
import com.wasd.qlic.monitoring.domain.model.TankLevelLowEvent
import com.wasd.qlic.notifications.domain.model.NotificationCategory
import com.wasd.qlic.notifications.domain.model.PushMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.math.BigDecimal
import java.util.Locale

/**
 * Turns the domain events of Alerting and Device Monitoring into push notifications. They run after the
 * transaction that raised the event commits, so a notification is never sent for an alert that was rolled
 * back, and a failure of the provider never undoes the alert.
 */
@Component
class NotificationEventListeners(
    private val pushService: PushNotificationService,
    private val messageSource: MessageSource,
    @Value("\${qlic.push.locale:es-419}") localeTag: String
) {
    private val log = LoggerFactory.getLogger(NotificationEventListeners::class.java)
    private val locale: Locale = Locale.forLanguageTag(localeTag)

    /** US15, US19: leak alert to the subscriber and to the authorized recipients. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onLeakAlertRaised(event: LeakAlertRaisedEvent) = notify {
        pushService.sendToAccount(
            event.accountId,
            PushMessage(
                title = message("push.leak.title"),
                body = message("push.leak.body", urgency(event.urgency), plain(event.estimatedVolumeLiters)),
                category = NotificationCategory.LEAK_ALERT,
                data = mapOf("alertId" to event.alertId.toString(), "waterPointId" to event.waterPointId.toString())
            ),
            event.recipientPushTokens
        )
    }

    /** US29 - Scenario 1: push with the affected tank and its current level. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onTankLevelLow(event: TankLevelLowEvent) = notify {
        pushService.sendToAccount(
            event.accountId,
            PushMessage(
                title = message("push.tank-low.title", event.tankName),
                body = message(
                    "push.tank-low.body", event.tankName, plain(event.levelPercentage), plain(event.criticalLevelPercentage)
                ),
                category = NotificationCategory.TANK_LEVEL_LOW,
                data = mapOf("tankId" to event.tankId.toString(), "levelPercentage" to plain(event.levelPercentage))
            )
        )
    }

    private fun notify(block: () -> Unit) {
        try {
            block()
        } catch (exception: RuntimeException) {
            log.error("The push notification could not be processed", exception)
        }
    }

    private fun urgency(value: String): String = message("push.urgency.$value")

    private fun message(key: String, vararg arguments: Any): String =
        messageSource.getMessage(key, arguments, key, locale) ?: key

    private fun plain(value: BigDecimal?): String = value?.stripTrailingZeros()?.toPlainString() ?: "-"
}
