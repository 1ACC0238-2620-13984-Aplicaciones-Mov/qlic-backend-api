package com.wasd.qlic.consumption.domain.services

import com.wasd.qlic.consumption.domain.model.ActivitySchedule
import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.ReadingSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ConsumptionAggregationServiceTest {

    private val service = ConsumptionAggregationService()
    private val accountId = UUID.randomUUID()
    private val kitchen = UUID.randomUUID()
    private val bathroom = UUID.randomUUID()
    private val period = Period(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8))
    private val weekdays = ActivitySchedule(
        LocalTime.of(8, 0), LocalTime.of(20, 0),
        setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    )

    private fun reading(waterPoint: UUID, liters: String, at: String) =
        ReadingSnapshot(waterPoint, BigDecimal(liters), LocalDateTime.parse(at))

    @Test
    @DisplayName("US24 - Scenario 1: the consumption is differentiated inside and outside the activity schedule")
    fun shouldSplitConsumptionByActivityHours() {
        val readings = listOf(
            reading(kitchen, "100", "2026-11-02T10:00:00"), // Monday, open
            reading(kitchen, "20", "2026-11-02T23:30:00"),  // Monday, closed
            reading(bathroom, "30", "2026-11-07T11:00:00"), // Saturday, not an active day
            reading(bathroom, "50", "2026-11-04T19:59:00")  // Wednesday, open
        )

        val report = service.aggregateByActivity(accountId, period, readings, weekdays)

        assertTrue(report.scheduleDefined)
        assertEquals(BigDecimal("200.00"), report.totalLiters)
        assertEquals(BigDecimal("150.00"), report.withinActivityLiters)
        assertEquals(BigDecimal("50.00"), report.outsideActivityLiters)
        assertEquals(BigDecimal("25.00"), report.outsideActivityPercentage)
        val bathroomResult = report.byWaterPoint.first { it.waterPointId == bathroom }
        assertEquals(BigDecimal("30.00"), bathroomResult.outsideActivityLiters)
    }

    @Test
    @DisplayName("US24 - Business rule: without a declared schedule only the total is reported")
    fun shouldReportOnlyTotalsWithoutSchedule() {
        val report = service.aggregateByActivity(accountId, period, listOf(reading(kitchen, "40", "2026-11-03T03:00:00")), null)

        assertFalse(report.scheduleDefined)
        assertEquals(BigDecimal("40.00"), report.totalLiters)
        assertNull(report.outsideActivityLiters)
    }

    @Test
    @DisplayName("US24: a schedule that crosses midnight counts the early hours in the previous active day")
    fun shouldSupportOvernightSchedules() {
        val nightBar = ActivitySchedule(LocalTime.of(18, 0), LocalTime.of(2, 0), setOf(DayOfWeek.FRIDAY))

        assertTrue(nightBar.isWithinActivity(LocalDateTime.parse("2026-11-06T22:00:00")))  // Friday night
        assertTrue(nightBar.isWithinActivity(LocalDateTime.parse("2026-11-07T01:30:00")))  // Saturday early, Friday shift
        assertFalse(nightBar.isWithinActivity(LocalDateTime.parse("2026-11-07T03:00:00"))) // after closing
        assertFalse(nightBar.isWithinActivity(LocalDateTime.parse("2026-11-06T01:00:00"))) // Thursday shift not active
    }
}
