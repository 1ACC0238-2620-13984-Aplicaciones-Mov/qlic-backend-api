package com.wasd.qlic.alerting.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class LeakAlert(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val waterPointId: UUID,
    var state: AlertState = AlertState.ACTIVE,
    var urgency: UrgencyLevel,
    val detectedAt: LocalDateTime = LocalDateTime.now(),
    var estimatedVolumeLiters: BigDecimal,
    var estimatedCostSoles: BigDecimal,
    val recommendedAction: String,
    var acknowledgedAction: String? = null,
    var previousAlertId: UUID? = null
) {
    fun isActive(): Boolean = this.state == AlertState.ACTIVE

    fun acknowledge(action: String) {
        this.state = AlertState.ACKNOWLEDGED
        this.acknowledgedAction = action
    }

    fun escalate(newUrgency: UrgencyLevel, additionalVolume: BigDecimal, additionalCost: BigDecimal) {
        this.urgency = newUrgency
        this.estimatedVolumeLiters = this.estimatedVolumeLiters.add(additionalVolume)
        this.estimatedCostSoles = this.estimatedCostSoles.add(additionalCost)
    }
}