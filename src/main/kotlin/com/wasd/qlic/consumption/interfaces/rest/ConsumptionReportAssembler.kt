package com.wasd.qlic.consumption.interfaces.rest

import com.wasd.qlic.consumption.domain.model.ComparisonTrend
import com.wasd.qlic.consumption.domain.model.GoalProgress
import com.wasd.qlic.consumption.domain.model.Money
import com.wasd.qlic.consumption.domain.model.PeriodComparison
import com.wasd.qlic.consumption.domain.model.PeriodConsumptionReport
import com.wasd.qlic.consumption.domain.model.SavingRecommendation
import com.wasd.qlic.consumption.domain.model.WaterTariff
import com.wasd.qlic.consumption.interfaces.rest.resources.EstimatedCostResource
import com.wasd.qlic.consumption.interfaces.rest.resources.PeriodComparisonResource
import com.wasd.qlic.consumption.interfaces.rest.resources.PeriodConsumptionReportResource
import com.wasd.qlic.consumption.interfaces.rest.resources.SavingRecommendationResource
import com.wasd.qlic.consumption.interfaces.rest.resources.ShareableReportResource
import com.wasd.qlic.consumption.interfaces.rest.resources.SustainabilityGoalResource
import com.wasd.qlic.consumption.interfaces.rest.resources.TariffResource
import com.wasd.qlic.consumption.interfaces.rest.resources.WaterPointConsumptionResource
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

/** Translates the report value objects into resources, with the texts localized by Accept-Language. */
@Component
class ConsumptionReportAssembler(private val messageSource: MessageSource) {

    fun toResource(report: PeriodConsumptionReport) = PeriodConsumptionReportResource(
        from = report.period.from,
        to = report.period.to,
        hasData = report.hasData,
        message = if (report.hasData) null else message("consumption.report.no-data"),
        totalLiters = report.totalLiters,
        totalCubicMeters = report.totalLiters.divide(BigDecimal(1000), 2, RoundingMode.HALF_UP),
        estimatedCost = toResource(report.estimatedCost),
        tariff = toResource(report.tariff),
        byWaterPoint = report.byWaterPoint.map {
            WaterPointConsumptionResource(
                it.waterPointId, it.waterPointName, it.waterPointType, it.liters, it.percentage, toResource(it.estimatedCost)
            )
        },
        recommendationsAvailable = report.recommendationsAvailable,
        recommendations = report.recommendations.map { toResource(it) },
        goal = report.goal?.let { toResource(it) }
    )

    fun toResource(comparison: PeriodComparison) = PeriodComparisonResource(
        currentFrom = comparison.current.from,
        currentTo = comparison.current.to,
        currentLiters = comparison.currentLiters,
        previousFrom = comparison.previous.from,
        previousTo = comparison.previous.to,
        previousLiters = comparison.previousLiters,
        comparable = comparison.comparable,
        variationLiters = comparison.variationLiters,
        variationPercentage = comparison.variationPercentage,
        trend = comparison.trend,
        message = when (comparison.trend) {
            null -> message("consumption.comparison.not-comparable")
            ComparisonTrend.INCREASING -> message("consumption.comparison.increasing", plain(comparison.variationPercentage))
            ComparisonTrend.DECREASING -> message("consumption.comparison.decreasing", plain(comparison.variationPercentage?.abs()))
            ComparisonTrend.STABLE -> message("consumption.comparison.stable", plain(comparison.variationPercentage))
        }
    )

    fun toResource(tariff: WaterTariff) = TariffResource(tariff.category, tariff.pricePerCubicMeter, tariff.custom)

    fun toResource(progress: GoalProgress) = SustainabilityGoalResource(
        month = progress.goal.month.toString(),
        reductionPercentage = progress.goal.reductionPercentage,
        baselineLiters = progress.goal.baselineLiters,
        targetLiters = progress.goal.targetLiters,
        consumedLiters = progress.consumedLiters,
        targetUsedPercentage = progress.targetUsedPercentage,
        monthElapsedPercentage = progress.monthElapsedPercentage,
        status = progress.status,
        message = message("consumption.goal.status.${progress.status.name}", plain(progress.targetUsedPercentage))
    )

    /** US26: plain-text summary of the period, ready for the share sheet of the device. */
    fun toShareable(report: PeriodConsumptionReport): ShareableReportResource {
        val title = message("consumption.share.title", report.period.from.toString(), report.period.to.toString())
        if (!report.hasData) return ShareableReportResource(title, listOf(title, message("consumption.report.no-data")).joinToString("\n"))
        val lines = mutableListOf(
            title,
            message(
                "consumption.share.total",
                plain(report.totalLiters),
                plain(report.totalLiters.divide(BigDecimal(1000), 2, RoundingMode.HALF_UP))
            ),
            message("consumption.share.cost", plain(report.estimatedCost.amount))
        )
        report.byWaterPoint.firstOrNull()?.let {
            lines += message("consumption.share.top", it.waterPointName ?: it.waterPointId.toString(), plain(it.liters), plain(it.percentage))
        }
        report.goal?.let { lines += message("consumption.share.goal", plain(it.targetUsedPercentage)) }
        lines += message("consumption.share.footer")
        return ShareableReportResource(title, lines.joinToString("\n"))
    }

    private fun toResource(money: Money) = EstimatedCostResource(money.amount, money.currency)

    private fun toResource(recommendation: SavingRecommendation): SavingRecommendationResource {
        val name = recommendation.waterPointName ?: message("consumption.recommendation.unknown-water-point")
        val advice = message("consumption.recommendation.type.${recommendation.waterPointType ?: "OTHER"}")
        val context = recommendation.increasePercentage
            ?.let { message("consumption.recommendation.above-baseline", name, plain(it)) }
            ?: message("consumption.recommendation.top", name)
        return SavingRecommendationResource(
            waterPointId = recommendation.waterPointId,
            waterPointName = recommendation.waterPointName,
            dailyAverageLiters = recommendation.dailyAverageLiters,
            baselineDailyAverageLiters = recommendation.baselineDailyAverageLiters,
            increasePercentage = recommendation.increasePercentage,
            message = "$context $advice"
        )
    }

    private fun message(key: String, vararg arguments: Any): String =
        messageSource.getMessage(key, arguments, key, LocaleContextHolder.getLocale()) ?: key

    /** Numbers are passed as plain text so that MessageFormat does not regroup or round them. */
    private fun plain(value: BigDecimal?): String = value?.stripTrailingZeros()?.toPlainString() ?: "0"
}
