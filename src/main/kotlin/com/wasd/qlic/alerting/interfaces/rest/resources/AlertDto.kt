package com.wasd.qlic.alerting.interfaces.rest.resources

import com.wasd.qlic.alerting.domain.model.AlertState
import com.wasd.qlic.alerting.domain.model.ThresholdSource
import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class EvaluateReadingRequest(
    val accountId: UUID,
    val waterPointId: UUID,
    val currentFlowLitersPerHour: BigDecimal,
    val thresholdLimit: BigDecimal? = null,
    val isSustained: Boolean
)

data class LeakAlertResource(
    val id: UUID,
    val accountId: UUID,
    val waterPointId: UUID,
    val state: AlertState,
    val urgency: UrgencyLevel,
    val detectedAt: LocalDateTime,
    val estimatedVolumeLiters: BigDecimal,
    val estimatedCostSoles: BigDecimal,
    val recommendedAction: String,
    val acknowledgedAction: String? = null,
    val previousAlertId: UUID? = null,
    val pushDispatched: Boolean = true,
    val notifiedRecipientsCount: Int = 0
)

// US17 DTOs
data class ConfigureThresholdRequest(
    val accountId: UUID,
    val waterPointId: UUID,
    val period: String = "HOURLY",
    val customLimitLitersPerHour: BigDecimal? = null,
    val baselineLitersPerHour: BigDecimal = BigDecimal("100.00")
)

data class ConsumptionThresholdResource(
    val id: UUID,
    val accountId: UUID,
    val waterPointId: UUID,
    val period: String,
    val customLimitLitersPerHour: BigDecimal?,
    val baselineLitersPerHour: BigDecimal,
    val effectiveLimitLitersPerHour: BigDecimal,
    val source: ThresholdSource,
    val updatedAt: LocalDateTime
)

// US18 DTOs
data class ConfigureNotificationPreferenceRequest(
    val accountId: UUID,
    val groupingWindowMinutes: Int = 30,
    val minimumUrgency: UrgencyLevel = UrgencyLevel.INFORMATIONAL
)

data class NotificationPreferenceResource(
    val id: UUID,
    val accountId: UUID,
    val groupingWindowMinutes: Int,
    val minimumUrgency: UrgencyLevel,
    val updatedAt: LocalDateTime
)

// US19 DTOs
data class AddAuthorizedRecipientRequest(
    val accountId: UUID,
    val fullName: String,
    val email: String,
    val devicePushToken: String,
    val enabled: Boolean = true
)

data class ToggleAuthorizedRecipientRequest(
    val enabled: Boolean
)

data class AuthorizedRecipientResource(
    val id: UUID,
    val accountId: UUID,
    val fullName: String,
    val email: String,
    val devicePushToken: String,
    val enabled: Boolean,
    val lastNotifiedAt: LocalDateTime?
)

// US20 DTOs
data class AcknowledgeAlertRequest(
    val accountId: UUID,
    val actionTaken: String
)