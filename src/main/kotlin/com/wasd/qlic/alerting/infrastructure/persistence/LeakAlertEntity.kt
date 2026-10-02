package com.wasd.qlic.alerting.infrastructure.persistence

import com.wasd.qlic.alerting.domain.model.AlertState
import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "leak_alerts")
class LeakAlertEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "account_id", nullable = false)
    val accountId: UUID,

    @Column(name = "water_point_id", nullable = false)
    val waterPointId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var state: AlertState,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var urgency: UrgencyLevel,

    @Column(name = "detected_at", nullable = false)
    val detectedAt: LocalDateTime,

    @Column(name = "estimated_volume", nullable = false, precision = 12, scale = 2)
    var estimatedVolume: BigDecimal,

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    var estimatedCost: BigDecimal,

    @Column(name = "recommended_action", nullable = false, length = 255)
    val recommendedAction: String,

    @Column(name = "acknowledged_action", columnDefinition = "TEXT")
    var acknowledgedAction: String? = null,

    @Column(name = "previous_alert_id")
    var previousAlertId: UUID? = null
)