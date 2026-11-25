package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.queries.GetMonitoringDashboardQuery
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.domain.model.WaterPointType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class MonitoringDashboardQueryHandlerTest {

    private val accountId = UUID.randomUUID()
    private val now = LocalDateTime.of(2026, 11, 20, 15, 0)
    private lateinit var devices: InMemoryIoTDeviceRepository
    private lateinit var waterPoints: InMemoryWaterPointRepository
    private lateinit var locations: InMemoryLocationRepository
    private lateinit var readings: InMemoryConsumptionReadingRepository
    private lateinit var handler: MonitoringDashboardQueryHandler
    private lateinit var kitchen: WaterPoint
    private lateinit var garden: WaterPoint
    private lateinit var surco: Location

    @BeforeEach
    fun setUp() {
        devices = InMemoryIoTDeviceRepository()
        waterPoints = InMemoryWaterPointRepository()
        locations = InMemoryLocationRepository()
        readings = InMemoryConsumptionReadingRepository()
        handler = MonitoringDashboardQueryHandler(devices, waterPoints, locations, readings)

        val miraflores = locations.save(Location(accountId = accountId, name = "Miraflores"))
        surco = locations.save(Location(accountId = accountId, name = "Surco"))
        kitchen = waterPoints.save(WaterPoint(accountId = accountId, locationId = miraflores.id, name = "Cocina", type = WaterPointType.KITCHEN))
        garden = waterPoints.save(WaterPoint(accountId = accountId, locationId = surco.id, name = "Jardín", type = WaterPointType.GARDEN))

        devices.save(device("SN-1", kitchen.id, lastSyncAt = now.minusMinutes(5)))
        devices.save(device("SN-2", garden.id, lastSyncAt = now.minusHours(3)))
        devices.save(device("SN-3", null, lastSyncAt = now.minusMinutes(1)))

        reading(kitchen, "40", now.minusMinutes(10), flow = "6.5", pressure = "2.0")
        reading(kitchen, "60", now.minusDays(2), flow = "3.0", pressure = "1.8")
        reading(garden, "25", now.minusHours(3), flow = "4.0", pressure = "2.4")
        reading(garden, "500", now.minusMonths(1))
    }

    private fun device(serial: String, waterPointId: UUID?, lastSyncAt: LocalDateTime) = IoTDevice(
        serialNumber = serial, model = "QlicSense", qrCodePayload = "QR-$serial", accountId = accountId,
        waterPointId = waterPointId, lastSyncAt = lastSyncAt
    )

    private fun reading(waterPoint: WaterPoint, liters: String, at: LocalDateTime, flow: String? = null, pressure: String? = null) =
        readings.save(
            ConsumptionReading(
                accountId = accountId, deviceId = UUID.randomUUID(), waterPointId = waterPoint.id,
                volumeLiters = BigDecimal(liters), recordedAt = at,
                flowRateLitersPerMinute = flow?.let { BigDecimal(it) }, pressureBar = pressure?.let { BigDecimal(it) }
            )
        )

    @Test
    @DisplayName("The dashboard consolidates today, the month, live telemetry and the status of the devices")
    fun shouldConsolidateDashboard() {
        val dashboard = handler.handle(GetMonitoringDashboardQuery(accountId, now = now))

        assertEquals(BigDecimal("65.00"), dashboard.todayLiters)
        assertEquals(BigDecimal("125.00"), dashboard.monthLiters)
        assertEquals(BigDecimal("6.50"), dashboard.currentFlowLitersPerMinute)
        assertEquals(BigDecimal("2.00"), dashboard.averagePressureBar)
        assertEquals(3, dashboard.devices.total)
        assertEquals(2, dashboard.devices.online)
        assertEquals(1, dashboard.devices.offline)
        assertEquals(1, dashboard.devices.withoutWaterPoint)

        val kitchenTelemetry = dashboard.waterPoints.first { it.waterPointId == kitchen.id }
        assertEquals(BigDecimal("40.00"), kitchenTelemetry.todayLiters)
        assertEquals(BigDecimal("100.00"), kitchenTelemetry.monthLiters)
        assertEquals(BigDecimal("6.5"), kitchenTelemetry.lastFlowLitersPerMinute)
        assertEquals("Miraflores", kitchenTelemetry.locationName)
    }

    @Test
    @DisplayName("US13: with a location only its Water Points, readings and devices are consolidated")
    fun shouldFilterByLocation() {
        val dashboard = handler.handle(GetMonitoringDashboardQuery(accountId, surco.id, now))

        assertEquals(listOf(garden.id), dashboard.waterPoints.map { it.waterPointId })
        assertEquals(BigDecimal("25.00"), dashboard.todayLiters)
        assertEquals(BigDecimal("0.00"), dashboard.currentFlowLitersPerMinute)
        assertNull(dashboard.averagePressureBar)
        assertEquals(1, dashboard.devices.total)
        assertEquals(1, dashboard.devices.offline)
    }
}
