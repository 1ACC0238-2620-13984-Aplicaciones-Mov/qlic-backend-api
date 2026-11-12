package com.wasd.qlic.alerting.application.commands

import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import java.math.BigDecimal
import java.util.UUID

data class ConfigureThresholdCommand(
    val accountId: UUID,
    val waterPointId: UUID,
    val period: String = "HOURLY",
    val customLimitLitersPerHour: BigDecimal?,
    val baselineLitersPerHour: BigDecimal = BigDecimal("100.00")
)

data class ConfigureNotificationPreferenceCommand(
    val accountId: UUID,
    val groupingWindowMinutes: Int,
    val minimumUrgency: UrgencyLevel
)

data class AddAuthorizedRecipientCommand(
    val accountId: UUID,
    val fullName: String,
    val email: String,
    val devicePushToken: String,
    val enabled: Boolean = true
)

data class AcknowledgeAlertCommand(
    val alertId: UUID,
    val accountId: UUID,
    val actionTaken: String
)