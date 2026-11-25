package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.queries.GetMonitoringDashboardQuery
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.model.DeviceStatusSummary
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.MonitoringDashboard
import com.wasd.qlic.monitoring.domain.model.WaterPointTelemetry
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * GetDashboardQueryHandler: retrieves devices, Water Points and the readings of the current month and
 * consolidates consumption, flow, pressure and connection status, filtered by location when requested.
 */
@Component
@Transactional(readOnly = true)
class MonitoringDashboardQueryHandler(
    private val deviceRepository: IoTDeviceRepository,
    private val waterPointRepository: WaterPointRepository,
    private val locationRepository: LocationRepository,
    private val readingRepository: ConsumptionReadingRepository
) {
    fun handle(query: GetMonitoringDashboardQuery): MonitoringDashboard {
        val now = query.now
        val waterPoints = if (query.locationId != null) {
            ownedLocation(locationRepository, query.locationId, query.accountId)
            waterPointRepository.findByLocationId(query.locationId)
        } else {
            waterPointRepository.findByAccountId(query.accountId)
        }
        val waterPointIds = waterPoints.map { it.id }.toSet()
        val locationNames = locationRepository.findByAccountId(query.accountId).associate { it.id to it.name }

        val devices = deviceRepository.findByAccountId(query.accountId)
            .filter { query.locationId == null || it.waterPointId in waterPointIds }

        val monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay()
        val todayStart = now.toLocalDate().atStartOfDay()
        val readings = readingRepository.findByAccountIdBetween(query.accountId, monthStart, now)
            .filter { query.locationId == null || it.waterPointId in waterPointIds }
        val readingsByWaterPoint = readings.groupBy { it.waterPointId }
        val liveSince = now.minusMinutes(IoTDevice.DISCONNECTION_LIMIT_MINUTES)

        val telemetry = waterPoints.map { waterPoint ->
            val items = readingsByWaterPoint[waterPoint.id].orEmpty()
            val last = items.maxByOrNull { it.recordedAt }
            WaterPointTelemetry(
                waterPointId = waterPoint.id,
                name = waterPoint.name,
                type = waterPoint.type,
                locationId = waterPoint.locationId,
                locationName = locationNames[waterPoint.locationId],
                todayLiters = items.filter { !it.recordedAt.isBefore(todayStart) }.sumOf { it.volumeLiters }.scaled(),
                monthLiters = items.sumOf { it.volumeLiters }.scaled(),
                lastFlowLitersPerMinute = items.lastWith { it.flowRateLitersPerMinute }?.flowRateLitersPerMinute,
                lastPressureBar = items.lastWith { it.pressureBar }?.pressureBar,
                lastReadingAt = last?.recordedAt
            )
        }

        val liveReadings = readingsByWaterPoint.values
            .mapNotNull { items -> items.maxByOrNull { it.recordedAt } }
            .filter { !it.recordedAt.isBefore(liveSince) }
        val livePressures = liveReadings.mapNotNull { it.pressureBar }

        return MonitoringDashboard(
            locationId = query.locationId,
            generatedAt = now,
            todayLiters = readings.filter { !it.recordedAt.isBefore(todayStart) }.sumOf { it.volumeLiters }.scaled(),
            monthLiters = readings.sumOf { it.volumeLiters }.scaled(),
            currentFlowLitersPerMinute = liveReadings.mapNotNull { it.flowRateLitersPerMinute }
                .sumOf { it }.scaled(),
            averagePressureBar = if (livePressures.isEmpty()) null
            else livePressures.sumOf { it }
                .divide(BigDecimal(livePressures.size), 2, RoundingMode.HALF_UP),
            devices = summarize(devices, now),
            waterPoints = telemetry.sortedByDescending { it.todayLiters }
        )
    }

    private fun summarize(devices: List<IoTDevice>, now: java.time.LocalDateTime): DeviceStatusSummary {
        val statuses = devices.map { it.effectiveStatus(now) }
        return DeviceStatusSummary(
            total = devices.size,
            online = statuses.count { it == DeviceStatus.ONLINE || it == DeviceStatus.CALIBRATING },
            lowBattery = statuses.count { it == DeviceStatus.LOW_BATTERY },
            offline = statuses.count { it == DeviceStatus.OFFLINE },
            withoutWaterPoint = devices.count { it.waterPointId == null }
        )
    }

    /** Most recent reading of the list that reports the given telemetry value. */
    private inline fun List<ConsumptionReading>.lastWith(value: (ConsumptionReading) -> BigDecimal?): ConsumptionReading? =
        filter { value(it) != null }.maxByOrNull { it.recordedAt }

    private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
