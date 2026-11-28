package com.wasd.qlic.monitoring.domain

import com.wasd.qlic.monitoring.application.services.MeterSimulator
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.model.SimulationScenario
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class ConsumptionReadingFlowTest {

    private val account = UUID.randomUUID()
    private val device = UUID.randomUUID()
    private val waterPoint = UUID.randomUUID()
    private val start = LocalDateTime.of(2026, 11, 26, 9, 0)

    private fun reading(volume: String, at: LocalDateTime, flowPerMinute: String? = null) = ConsumptionReading(
        accountId = account, deviceId = device, waterPointId = waterPoint, volumeLiters = BigDecimal(volume),
        recordedAt = at, flowRateLitersPerMinute = flowPerMinute?.let { BigDecimal(it) }
    )

    @Test
    @DisplayName("The flow per hour uses the reported flow or, without it, the volume since the previous reading")
    fun flowPerHour() {
        assertEquals(BigDecimal("360.00"), reading("10", start, "6").flowLitersPerHour())
        assertEquals(BigDecimal("120.00"), reading("20", start.plusMinutes(10)).flowLitersPerHour(reading("5", start)))
        assertNull(reading("20", start).flowLitersPerHour())
        assertNull(reading("20", start.plusMinutes(45)).flowLitersPerHour(reading("5", start)))
    }

    @Test
    @DisplayName("The simulator keeps NORMAL below the default threshold and LEAK above it at any hour")
    fun simulatorScenarios() {
        val defaultLimitPerMinute = BigDecimal("125").divide(BigDecimal("60"), 3, java.math.RoundingMode.HALF_UP)
        val random = Random(1)
        (0..23).forEach { hour ->
            assertTrue(MeterSimulator.flowLitersPerMinute(SimulationScenario.NORMAL, hour, random) < defaultLimitPerMinute)
            assertTrue(MeterSimulator.flowLitersPerMinute(SimulationScenario.LEAK, hour, random) > defaultLimitPerMinute)
        }
    }
}
