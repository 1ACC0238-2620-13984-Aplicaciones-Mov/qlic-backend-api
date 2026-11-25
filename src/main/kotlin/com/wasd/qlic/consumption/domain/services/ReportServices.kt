package com.wasd.qlic.consumption.domain.services

import com.wasd.qlic.consumption.domain.model.ComparisonTrend
import com.wasd.qlic.consumption.domain.model.Money
import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.PeriodComparison
import com.wasd.qlic.consumption.domain.model.ReadingSnapshot
import com.wasd.qlic.consumption.domain.model.SavingRecommendation
import com.wasd.qlic.consumption.domain.model.ServiceCategory
import com.wasd.qlic.consumption.domain.model.WaterTariff
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Name and kind of a Water Point, translated from Device Monitoring. */
data class WaterPointInfo(val name: String, val type: String)

/** Domain port: catalog of the Water Points of an account, provided by Device Monitoring. */
interface WaterPointCatalogProvider {
    fun waterPoints(accountId: UUID): Map<UUID, WaterPointInfo>
}

/** Domain port: service category that corresponds to the plan of the account, provided by Subscriptions. */
interface ServiceCategoryProvider {
    fun defaultCategory(accountId: UUID): ServiceCategory
}

/** Domain port: tariffs in force per service category, in soles per cubic meter. */
interface TariffCatalog {
    fun pricePerCubicMeter(category: ServiceCategory): BigDecimal
}

/** CostEstimationService: applies the tariff in force to the volume; the result is always referential (US23). */
@Service
class CostEstimationService {
    fun estimate(liters: BigDecimal, tariff: WaterTariff): Money =
        Money.soles(liters.divide(LITERS_PER_CUBIC_METER, 6, RoundingMode.HALF_UP).multiply(tariff.pricePerCubicMeter))

    private companion object {
        val LITERS_PER_CUBIC_METER = BigDecimal(1000)
    }
}

/**
 * PeriodComparison is built only when both periods have readings (US22 - Scenario 2). A variation
 * within ±5 % is considered stable.
 */
@Service
class PeriodComparisonService {
    fun compare(current: Period, currentReadings: List<ReadingSnapshot>, previous: Period, previousReadings: List<ReadingSnapshot>): PeriodComparison {
        val currentLiters = currentReadings.sumOf { it.volumeLiters }.scaled()
        val previousLiters = previousReadings.sumOf { it.volumeLiters }.scaled()
        val comparable = currentReadings.isNotEmpty() && previousReadings.isNotEmpty() && previousLiters.signum() > 0
        if (!comparable) {
            return PeriodComparison(current, currentLiters, previous, previousLiters, false, null, null, null)
        }
        val variation = currentLiters - previousLiters
        val percentage = variation.multiply(BigDecimal(100)).divide(previousLiters, 2, RoundingMode.HALF_UP)
        val trend = when {
            percentage > STABLE_MARGIN -> ComparisonTrend.INCREASING
            percentage < STABLE_MARGIN.negate() -> ComparisonTrend.DECREASING
            else -> ComparisonTrend.STABLE
        }
        return PeriodComparison(current, currentLiters, previous, previousLiters, true, variation.scaled(), percentage, trend)
    }

    private companion object {
        val STABLE_MARGIN = BigDecimal(5)
    }
}

/**
 * RecommendationService (US25): the baseline of each Water Point is its daily average in the previous
 * 30 days. With at least 7 days of history, recommendations are generated for the Water Points with the
 * highest consumption of the period, indicating how much they exceed their baseline.
 */
@Service
class RecommendationService {
    fun recommend(
        period: Period,
        periodReadings: List<ReadingSnapshot>,
        baselineReadings: List<ReadingSnapshot>,
        catalog: Map<UUID, WaterPointInfo>
    ): List<SavingRecommendation>? {
        val baselineDays = baselineReadings.map { it.recordedAt.toLocalDate() }.distinct().size
        if (baselineDays < MIN_BASELINE_DAYS || periodReadings.isEmpty()) return null

        val periodDays = BigDecimal(ChronoUnit.DAYS.between(period.from, period.to) + 1)
        val baselineByWaterPoint = baselineReadings.groupBy { it.waterPointId }
            .mapValues { (_, items) -> items.sumOf { it.volumeLiters }.divide(BigDecimal(BASELINE_DAYS), 2, RoundingMode.HALF_UP) }

        return periodReadings.groupBy { it.waterPointId }
            .mapValues { (_, items) -> items.sumOf { it.volumeLiters } }
            .entries.sortedByDescending { it.value }
            .take(MAX_RECOMMENDATIONS)
            .map { (waterPointId, liters) ->
                val daily = liters.divide(periodDays, 2, RoundingMode.HALF_UP)
                val baseline = baselineByWaterPoint[waterPointId] ?: BigDecimal.ZERO.setScale(2)
                val increase = if (baseline.signum() > 0 && daily > baseline)
                    (daily - baseline).multiply(BigDecimal(100)).divide(baseline, 2, RoundingMode.HALF_UP) else null
                SavingRecommendation(
                    waterPointId = waterPointId,
                    waterPointName = catalog[waterPointId]?.name,
                    waterPointType = catalog[waterPointId]?.type,
                    dailyAverageLiters = daily,
                    baselineDailyAverageLiters = baseline,
                    increasePercentage = increase
                )
            }
    }

    companion object {
        const val BASELINE_DAYS = 30L
        const val MIN_BASELINE_DAYS = 7
        const val MAX_RECOMMENDATIONS = 3
    }
}

private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
