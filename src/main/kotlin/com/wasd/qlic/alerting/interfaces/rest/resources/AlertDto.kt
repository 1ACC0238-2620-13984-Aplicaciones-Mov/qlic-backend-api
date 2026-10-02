package com.wasd.qlic.alerting.interfaces.rest.resources

import com.wasd.qlic.alerting.domain.model.AlertState
import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class EvaluateReadingRequest(
    val accountId: UUID,
    val waterPointId: UUID,
    val currentFlowLitersPerHour: BigDecimal,
    val thresholdLimit: BigDecimal,
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
    val recommendedAction: String
)