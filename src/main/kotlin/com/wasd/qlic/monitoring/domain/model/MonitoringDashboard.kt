package com.wasd.qlic.monitoring.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** Value Object: consumption and latest telemetry of one Water Point shown in the monitoring dashboard. */
data class WaterPointTelemetry(
    val waterPointId: UUID,
    val name: String,
    val type: WaterPointType,
    val locationId: UUID,
    val locationName: String?,
    val todayLiters: BigDecimal,
    val monthLiters: BigDecimal,
    val lastFlowLitersPerMinute: BigDecimal?,
    val lastPressureBar: BigDecimal?,
    val lastReadingAt: LocalDateTime?
)

/** Value Object: connection status of the devices of the dashboard (DeviceHealthService rule). */
data class DeviceStatusSummary(
    val total: Int,
    val online: Int,
    val lowBattery: Int,
    val offline: Int,
    val withoutWaterPoint: Int
)

/**
 * Value Object: consolidated view of the monitoring dashboard of an account, optionally restricted to
 * one location (US13). Current flow and pressure only consider readings received within the
 * disconnection limit, so stale values are never presented as live.
 */
data class MonitoringDashboard(
    val locationId: UUID?,
    val generatedAt: LocalDateTime,
    val todayLiters: BigDecimal,
    val monthLiters: BigDecimal,
    val currentFlowLitersPerMinute: BigDecimal,
    val averagePressureBar: BigDecimal?,
    val devices: DeviceStatusSummary,
    val waterPoints: List<WaterPointTelemetry>
)
