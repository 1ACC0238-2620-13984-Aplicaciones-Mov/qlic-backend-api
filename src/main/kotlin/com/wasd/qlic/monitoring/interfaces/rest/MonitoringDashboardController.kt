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
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.ZoneId
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
    @field:Schema(description = "When the server built this view; the app shows it as the age of its offline copy (US14)")
    val generatedAt: LocalDateTime,
    @field:Schema(description = "Most recent reading included in the view; null when there are no readings this month", nullable = true)
    val lastReadingAt: LocalDateTime?,
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
            "telemetry of each Water Point. With locationId only that location is consolidated. Offline support (US14): the " +
            "response carries an ETag and, when there are readings, Last-Modified; the app stores the copy and sends the ETag " +
            "in If-None-Match to receive 304 Not Modified when nothing changed."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Dashboard with the ETag and Last-Modified headers for the offline copy"),
        ApiResponse(responseCode = "304", description = "If-None-Match matches: the copy stored by the app is still current"),
        ApiResponse(responseCode = "404", description = "The location does not belong to the account")
    )
    fun dashboard(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Parameter(description = "Only consolidate this location (US13)") @RequestParam(required = false) locationId: UUID?,
        @Parameter(description = "ETag of the copy stored by the app (US14)", example = "\"5f2c9a1e0b7d4c3a\"")
        @RequestHeader(HttpHeaders.IF_NONE_MATCH, required = false) ifNoneMatch: String?
    ): ResponseEntity<MonitoringDashboardResource> {
        val dashboard = dashboardHandler.handle(GetMonitoringDashboardQuery(jwt.accountId(), locationId)).toResource()
        val eTag = dashboard.eTag()
        val cacheControl = CacheControl.noCache().cachePrivate()
        if (ifNoneMatch != null && ifNoneMatch.split(",").any { it.trim().removePrefix("W/") == eTag }) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).cacheControl(cacheControl).build()
        }
        val response = ResponseEntity.ok().eTag(eTag).cacheControl(cacheControl)
        dashboard.lastReadingAt?.let { response.lastModified(it.atZone(ZoneId.systemDefault())) }
        return response.body(dashboard)
    }

    /**
     * US14: fingerprint of the data of the view, without generatedAt, so the app can revalidate its offline
     * copy with If-None-Match and receive 304 when nothing changed.
     */
    private fun MonitoringDashboardResource.eTag(): String {
        val fingerprint = buildString {
            append(locationId).append('|').append(lastReadingAt).append('|')
            append(todayLiters.toPlainString()).append('|').append(monthLiters.toPlainString()).append('|')
            append(currentFlowLitersPerMinute.toPlainString()).append('|').append(averagePressureBar?.toPlainString()).append('|')
            append(devices.total).append(',').append(devices.online).append(',').append(devices.lowBattery).append(',')
            append(devices.offline).append(',').append(devices.withoutWaterPoint)
            waterPoints.sortedBy { it.waterPointId }.forEach {
                append('|').append(it.waterPointId).append(',').append(it.name).append(',')
                append(it.todayLiters.toPlainString()).append(',').append(it.monthLiters.toPlainString()).append(',')
                append(it.lastReadingAt)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(fingerprint.toByteArray(Charsets.UTF_8))
        return "\"" + digest.take(8).joinToString("") { "%02x".format(it) } + "\""
    }

    private fun MonitoringDashboard.toResource() = MonitoringDashboardResource(
        locationId = locationId,
        generatedAt = generatedAt,
        lastReadingAt = waterPoints.mapNotNull { it.lastReadingAt }.reduceOrNull { latest, next -> if (next.isAfter(latest)) next else latest },
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
