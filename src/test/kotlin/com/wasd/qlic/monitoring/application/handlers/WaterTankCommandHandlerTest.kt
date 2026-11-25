package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.RegisterTankLevelCommand
import com.wasd.qlic.monitoring.application.commands.SaveWaterTankCommand
import com.wasd.qlic.monitoring.application.queries.GetWaterTankQuery
import com.wasd.qlic.monitoring.domain.exceptions.InvalidWaterTankException
import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.TankLevelLowEvent
import com.wasd.qlic.monitoring.domain.model.TankStatus
import com.wasd.qlic.monitoring.domain.services.ReplenishmentPredictionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class WaterTankCommandHandlerTest {

    private val accountId = UUID.randomUUID()
    private val start = LocalDateTime.of(2026, 11, 18, 8, 0)
    private val events = mutableListOf<Any>()
    private lateinit var handler: WaterTankCommandHandler
    private lateinit var tankId: UUID

    @BeforeEach
    fun setUp() {
        val locations = InMemoryLocationRepository()
        val location = locations.save(Location(accountId = accountId, name = "Casa"))
        handler = WaterTankCommandHandler(
            InMemoryWaterTankRepository(),
            InMemoryTankLevelReadingRepository(),
            locations,
            InMemoryIoTDeviceRepository(),
            ReplenishmentPredictionService(),
            ApplicationEventPublisher { events.add(it) }
        )
        tankId = handler.handle(
            SaveWaterTankCommand(accountId, location.id, "Tanque elevado", BigDecimal(1000), BigDecimal(20), null)
        ).id
    }

    private fun level(value: String, at: LocalDateTime) =
        handler.handle(RegisterTankLevelCommand(tankId, accountId, BigDecimal(value), at))

    @Test
    @DisplayName("US28 - Scenario 1: the current level, the available liters and the last reading are kept")
    fun shouldKeepCurrentLevel() {
        level("80", start)
        val result = level("64.5", start.plusHours(2))

        assertEquals(BigDecimal("64.5"), result.tank.currentLevelPercentage)
        assertEquals(BigDecimal("645.00"), result.tank.availableLiters)
        assertEquals(start.plusHours(2), result.tank.lastReadingAt)
        assertEquals(TankStatus.NORMAL, result.tank.status)
    }

    @Test
    @DisplayName("US29 - Scenario 1: crossing the critical level publishes TankLevelLowEvent only once until refilled")
    fun shouldRaiseLowLevelOnce() {
        level("40", start)
        assertTrue(level("18", start.plusHours(1)).lowLevelAlertRaised)
        assertFalse(level("15", start.plusHours(2)).lowLevelAlertRaised)
        assertFalse(level("90", start.plusHours(3)).lowLevelAlertRaised)
        assertTrue(level("19", start.plusHours(10)).lowLevelAlertRaised)

        val lowEvents = events.filterIsInstance<TankLevelLowEvent>()
        assertEquals(2, lowEvents.size)
        assertEquals("Tanque elevado", lowEvents[0].tankName)
        assertEquals(BigDecimal("18"), lowEvents[0].levelPercentage)
    }

    @Test
    @DisplayName("US30 - Scenario 1: with a falling level the moment of reaching the critical level is predicted")
    fun shouldPredictReplenishment() {
        level("100", start)
        level("85", start.plusHours(12))
        level("70", start.plusHours(24))

        val prediction = handler.handle(GetWaterTankQuery(tankId, accountId)).prediction

        assertTrue(prediction.available)
        assertEquals(BigDecimal("1.25"), prediction.consumptionPercentagePerHour)
        assertEquals(BigDecimal("40.0"), prediction.hoursToCritical)
        assertEquals(start.plusHours(64), prediction.predictedCriticalAt)
    }

    @Test
    @DisplayName("US30 - Scenario 2: without enough history the prediction is not available")
    fun shouldNotPredictWithoutHistory() {
        level("100", start)
        level("95", start.plusHours(2))

        assertFalse(handler.handle(GetWaterTankQuery(tankId, accountId)).prediction.available)
    }

    @Test
    @DisplayName("A refill restarts the consumption pattern used by the prediction")
    fun shouldIgnoreReadingsBeforeRefill() {
        level("60", start)
        level("30", start.plusHours(12))
        level("100", start.plusHours(13))
        level("95", start.plusHours(20))

        assertFalse(handler.handle(GetWaterTankQuery(tankId, accountId)).prediction.available)
    }

    @Test
    @DisplayName("A level outside 0-100 is rejected")
    fun shouldRejectInvalidLevel() {
        assertThrows(InvalidWaterTankException::class.java) { level("120", start) }
    }
}
