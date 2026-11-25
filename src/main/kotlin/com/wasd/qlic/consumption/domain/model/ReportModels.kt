package com.wasd.qlic.consumption.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

/** Service category of the subscriber, which determines the applicable water tariff (US23). */
enum class ServiceCategory { DOMESTIC, COMMERCIAL }

/** Value Object: monetary amount with its currency, used for the estimated cost. */
data class Money(val amount: BigDecimal, val currency: String = "PEN") {
    companion object {
        fun soles(amount: BigDecimal) = Money(amount.setScale(2, RoundingMode.HALF_UP))
    }
}

/** Entity: tariff in force for a service category, in soles per cubic meter (water and sewerage). */
data class WaterTariff(val category: ServiceCategory, val pricePerCubicMeter: BigDecimal, val custom: Boolean)

/** Tariff preferences of an account: its service category and, optionally, its own price per m³. */
class TariffSettings(
    val accountId: UUID,
    var category: ServiceCategory,
    var customPricePerCubicMeter: BigDecimal? = null,
    var updatedAt: Instant = Instant.now()
) {
    init {
        validate(customPricePerCubicMeter)
    }

    fun redefine(category: ServiceCategory, customPricePerCubicMeter: BigDecimal?) {
        validate(customPricePerCubicMeter)
        this.category = category
        this.customPricePerCubicMeter = customPricePerCubicMeter
        updatedAt = Instant.now()
    }

    private fun validate(price: BigDecimal?) {
        require(price == null || price.signum() > 0) { "The price per cubic meter must be positive" }
    }
}

/** Volume consumed by a Water Point in a period, with its share of the total and its estimated cost. */
data class WaterPointConsumption(
    val waterPointId: UUID,
    val waterPointName: String?,
    val waterPointType: String?,
    val liters: BigDecimal,
    val percentage: BigDecimal,
    val estimatedCost: Money
)

/** Recommendation generated for one of the Water Points with the highest consumption of the period (US25). */
data class SavingRecommendation(
    val waterPointId: UUID,
    val waterPointName: String?,
    val waterPointType: String?,
    val dailyAverageLiters: BigDecimal,
    val baselineDailyAverageLiters: BigDecimal,
    val increasePercentage: BigDecimal?
)

enum class ComparisonTrend { INCREASING, DECREASING, STABLE }

/** Value Object: variation between two periods, in volume and percentage (US22). */
data class PeriodComparison(
    val current: Period,
    val currentLiters: BigDecimal,
    val previous: Period,
    val previousLiters: BigDecimal,
    val comparable: Boolean,
    val variationLiters: BigDecimal?,
    val variationPercentage: BigDecimal?,
    val trend: ComparisonTrend?
)

enum class GoalStatus { ON_TRACK, AT_RISK, EXCEEDED, ACHIEVED }

/** Entity: reduction goal defined by the subscriber for a month, compared with the previous month (US27). */
class SustainabilityGoal(
    val accountId: UUID,
    val month: YearMonth,
    reductionPercentage: BigDecimal,
    baselineLiters: BigDecimal,
    var updatedAt: Instant = Instant.now()
) {
    var reductionPercentage: BigDecimal = reductionPercentage
        private set
    var baselineLiters: BigDecimal = baselineLiters
        private set

    init {
        validate(reductionPercentage, baselineLiters)
    }

    val targetLiters: BigDecimal
        get() = baselineLiters.multiply(BigDecimal(100).subtract(reductionPercentage))
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

    fun redefine(reductionPercentage: BigDecimal, baselineLiters: BigDecimal) {
        validate(reductionPercentage, baselineLiters)
        this.reductionPercentage = reductionPercentage
        this.baselineLiters = baselineLiters
        updatedAt = Instant.now()
    }

    /**
     * Scenario 1: progress of the consumption against the target. Scenario 2: once the month is over, a
     * consumption at or below the target means that the goal was achieved.
     */
    fun progress(consumedLiters: BigDecimal, today: java.time.LocalDate): GoalProgress {
        val target = targetLiters
        val used = if (target.signum() == 0) BigDecimal.ZERO.setScale(2)
        else consumedLiters.multiply(BigDecimal(100)).divide(target, 2, RoundingMode.HALF_UP)
        val monthEnded = today.isAfter(month.atEndOfMonth())
        val elapsed = when {
            monthEnded -> BigDecimal(100)
            today.isBefore(month.atDay(1)) -> BigDecimal.ZERO
            else -> BigDecimal(today.dayOfMonth * 100).divide(BigDecimal(month.lengthOfMonth()), 2, RoundingMode.HALF_UP)
        }
        val status = when {
            consumedLiters > target -> GoalStatus.EXCEEDED
            monthEnded -> GoalStatus.ACHIEVED
            used > elapsed -> GoalStatus.AT_RISK
            else -> GoalStatus.ON_TRACK
        }
        return GoalProgress(this, consumedLiters.setScale(2, RoundingMode.HALF_UP), used, elapsed, status)
    }

    private fun validate(reduction: BigDecimal, baseline: BigDecimal) {
        require(reduction > BigDecimal.ZERO && reduction < BigDecimal(100)) { "The reduction must be between 0 and 100" }
        require(baseline.signum() > 0) { "The baseline must be positive" }
    }
}

/** Value Object: progress of a sustainability goal. */
data class GoalProgress(
    val goal: SustainabilityGoal,
    val consumedLiters: BigDecimal,
    val targetUsedPercentage: BigDecimal,
    val monthElapsedPercentage: BigDecimal,
    val status: GoalStatus
)

/**
 * Aggregate: consumption of an account in a period with its distribution by Water Point, estimated
 * cost (US21, US23), saving recommendations (US25) and the progress of the goal of the month (US27).
 */
data class PeriodConsumptionReport(
    val accountId: UUID,
    val period: Period,
    val readingsCount: Int,
    val totalLiters: BigDecimal,
    val estimatedCost: Money,
    val tariff: WaterTariff,
    val byWaterPoint: List<WaterPointConsumption>,
    val recommendationsAvailable: Boolean,
    val recommendations: List<SavingRecommendation>,
    val goal: GoalProgress?
) {
    val hasData: Boolean get() = readingsCount > 0
}
