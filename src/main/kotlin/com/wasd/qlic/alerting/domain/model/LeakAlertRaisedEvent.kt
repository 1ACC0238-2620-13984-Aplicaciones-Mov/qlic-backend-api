package com.wasd.qlic.alerting.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * Domain Event: a leak alert passed the notification preferences of the account and must reach the
 * subscriber and the enabled authorized recipients by push notification (US15, US19, TS06).
 */
data class LeakAlertRaisedEvent(
    val accountId: UUID,
    val alertId: UUID,
    val waterPointId: UUID,
    val urgency: String,
    val estimatedVolumeLiters: BigDecimal?,
    val recipientPushTokens: List<String>
)
