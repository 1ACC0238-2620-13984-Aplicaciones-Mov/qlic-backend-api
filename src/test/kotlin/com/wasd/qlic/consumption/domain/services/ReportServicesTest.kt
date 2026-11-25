package com.wasd.qlic.consumption.domain.services

import com.wasd.qlic.consumption.domain.model.ComparisonTrend
import com.wasd.qlic.consumption.domain.model.GoalStatus
import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.ReadingSnapshot
import com.wasd.qlic.consumption.domain.model.ServiceCategory
import com.wasd.qlic.consumption.domain.model.SustainabilityGoal
import com.wasd.qlic.consumption.domain.model.WaterTariff
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class ReportServicesTest {

    private val kitchen = UUID.randomUUID()
    private val garden = UUID.randomUUID()

    private fun reading(waterPointId: UUID, liters: String, date: LocalDate) =
        ReadingSnapshot(waterPointId, BigDecimal(liters), date.atTime(10, 0))

    @Test
    @DisplayName("US23: the cost applies the tariff per cubic meter to the volume")
    fun shouldEstimateCost() {
        val cost = CostEstimationService().estimate(BigDecimal("2500"), WaterTariff(ServiceCategory.DOMESTIC, BigDecimal("5.00"), false))

        assertEquals(BigDecimal("12.50"), cost.amount)
        assertEquals("PEN", cost.currency)
    }

    @Test
    @DisplayName("US22 - Scenario 1: the variation is expressed in volume and percentage")
    fun shouldCompareTwoPeriods() {
        val current = Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        val previous = Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))

        val comparison = PeriodComparisonService().compare(
            current, listOf(reading(kitchen, "1150", current.from)),
            previous, listOf(reading(kitchen, "1000", previous.from))
        )

        assertTrue(comparison.comparable)
        assertEquals(BigDecimal("150.00"), comparison.variationLiters)
        assertEquals(BigDecimal("15.00"), comparison.variationPercentage)
        assertEquals(ComparisonTrend.INCREASING, comparison.trend)
    }

    @Test
    @DisplayName("US22 - Scenario 2: without readings in the previous period there is no comparison")
    fun shouldNotCompareWithoutHistory() {
        val current = Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        val previous = Period(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10))

        val comparison = PeriodComparisonService().compare(current, listOf(reading(kitchen, "500", current.from)), previous, emptyList())

        assertFalse(comparison.comparable)
        assertNull(comparison.variationPercentage)
        assertNull(comparison.trend)
    }

    @Test
    @DisplayName("US25: with a baseline, the Water Points with the highest consumption get recommendations")
    fun shouldRecommendForHighestConsumers() {
        val period = Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))
        val baseline = (1..30).flatMap { day ->
            val date = LocalDate.of(2026, 10, day)
            listOf(reading(kitchen, "100", date), reading(garden, "50", date))
        }
        val current = (1..10).flatMap { day ->
            val date = LocalDate.of(2026, 11, day)
            listOf(reading(kitchen, "120", date), reading(garden, "40", date))
        }
        val catalog = mapOf(kitchen to WaterPointInfo("Cocina", "KITCHEN"), garden to WaterPointInfo("Jardín", "GARDEN"))

        val recommendations = RecommendationService().recommend(period, current, baseline, catalog)!!

        assertEquals(listOf(kitchen, garden), recommendations.map { it.waterPointId })
        assertEquals(BigDecimal("120.00"), recommendations[0].dailyAverageLiters)
        assertEquals(BigDecimal("100.00"), recommendations[0].baselineDailyAverageLiters)
        assertEquals(BigDecimal("20.00"), recommendations[0].increasePercentage)
        assertNull(recommendations[1].increasePercentage)
        assertEquals("Cocina", recommendations[0].waterPointName)
    }

    @Test
    @DisplayName("US25: without 7 days of history there is no baseline and no recommendation")
    fun shouldNotRecommendWithoutBaseline() {
        val period = Period(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10))

        val recommendations = RecommendationService().recommend(
            period, listOf(reading(kitchen, "120", period.from)), listOf(reading(kitchen, "100", LocalDate.of(2026, 10, 30))), emptyMap()
        )

        assertNull(recommendations)
    }

    @Test
    @DisplayName("US27: the goal reduces the previous month and is achieved when the month ends below the target")
    fun shouldTrackSustainabilityGoal() {
        val goal = SustainabilityGoal(UUID.randomUUID(), YearMonth.of(2026, 11), BigDecimal("10"), BigDecimal("4000"))

        assertEquals(BigDecimal("3600.00"), goal.targetLiters)
        assertEquals(GoalStatus.ON_TRACK, goal.progress(BigDecimal("1000"), LocalDate.of(2026, 11, 15)).status)
        assertEquals(GoalStatus.AT_RISK, goal.progress(BigDecimal("3000"), LocalDate.of(2026, 11, 15)).status)
        assertEquals(GoalStatus.EXCEEDED, goal.progress(BigDecimal("3700"), LocalDate.of(2026, 11, 20)).status)

        val achieved = goal.progress(BigDecimal("3500"), LocalDate.of(2026, 12, 1))
        assertEquals(GoalStatus.ACHIEVED, achieved.status)
        assertEquals(BigDecimal("97.22"), achieved.targetUsedPercentage)
    }

    @Test
    @DisplayName("US27: the reduction must be between 0 and 100")
    fun shouldRejectInvalidReduction() {
        assertThrows(IllegalArgumentException::class.java) {
            SustainabilityGoal(UUID.randomUUID(), YearMonth.of(2026, 11), BigDecimal("100"), BigDecimal("4000"))
        }
    }
}
