package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.handlers.MonitoringDashboardQueryHandler
import com.wasd.qlic.monitoring.application.queries.GetMonitoringDashboardQuery
import com.wasd.qlic.monitoring.domain.model.DeviceStatusSummary
import com.wasd.qlic.monitoring.domain.model.MonitoringDashboard
import com.wasd.qlic.monitoring.domain.model.WaterPointTelemetry
import com.wasd.qlic.monitoring.domain.model.WaterPointType
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Connection status of the devices (a device without readings in the last 60 minutes is OFFLINE).")
data class DeviceStatusSummaryResource(
    val total: Int,
    val online: Int,
    val lowBattery: Int,
    val offline: Int,
    val withoutWaterPoint: Int
)

@Schema(description = "Consumption and latest telemetry of a Water Point.")
data class WaterPointTelemetryResource(
    val waterPointId: UUID,
    val name: String,
    val type: WaterPointType,
    val locationId: UUID,
    val locationName: String?,
    @field:Schema(example = "125.40") val todayLiters: BigDecimal,
    @field:Schema(example = "3210.75") val monthLiters: BigDecimal,
    @field:Schema(example = "8.4", nullable = true) val lastFlowLitersPerMinute: BigDecimal?,
    @field:Schema(example = "2.1", nullable = true) val lastPressureBar: BigDecimal?,
    @field:Schema(nullable = true) val lastReadingAt: LocalDateTime?
)

@Schema(description = "Monitoring dashboard: consumption of today and of the month, live flow and pressure, and device status.")
data class MonitoringDashboardResource(
    @field:Schema(nullable = true) val locationId: UUID?,
    val generatedAt: LocalDateTime,
    @field:Schema(example = "540.25") val todayLiters: BigDecimal,
    @field:Schema(example = "12850.10") val monthLiters: BigDecimal,
    @field:Schema(example = "14.60", description = "Sum of the flow reported in the last 60 minutes") val currentFlowLitersPerMinute: BigDecimal,
    @field:Schema(example = "2.05", nullable = true, description = "Average pressure reported in the last 60 minutes") val averagePressureBar: BigDecimal?,
    val devices: DeviceStatusSummaryResource,
    val waterPoints: List<WaterPointTelemetryResource>
)

@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring Dashboard", description = "Device Monitoring - Consolidated view of consumption, flow, pressure and devices")
class MonitoringDashboardController(
    private val dashboardHandler: MonitoringDashboardQueryHandler
) {

    @GetMapping("/dashboard")
    @Operation(
        summary = "Monitoring dashboard of the account (US12, US13)",
        description = "Consumption of today and of the current month, live flow and pressure, the status of the devices and the " +
            "telemetry of each Water Point. With locationId only that location is consolidated."
    )
    fun dashboard(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) locationId: UUID?
    ): ResponseEntity<MonitoringDashboardResource> =
        ResponseEntity.ok(dashboardHandler.handle(GetMonitoringDashboardQuery(jwt.accountId(), locationId)).toResource())

    private fun MonitoringDashboard.toResource() = MonitoringDashboardResource(
        locationId = locationId,
        generatedAt = generatedAt,
        todayLiters = todayLiters,
        monthLiters = monthLiters,
        currentFlowLitersPerMinute = currentFlowLitersPerMinute,
        averagePressureBar = averagePressureBar,
        devices = devices.toResource(),
        waterPoints = waterPoints.map { it.toResource() }
    )

    private fun DeviceStatusSummary.toResource() =
        DeviceStatusSummaryResource(total, online, lowBattery, offline, withoutWaterPoint)

    private fun WaterPointTelemetry.toResource() = WaterPointTelemetryResource(
        waterPointId, name, type, locationId, locationName, todayLiters, monthLiters,
        lastFlowLitersPerMinute, lastPressureBar, lastReadingAt
    )
}
