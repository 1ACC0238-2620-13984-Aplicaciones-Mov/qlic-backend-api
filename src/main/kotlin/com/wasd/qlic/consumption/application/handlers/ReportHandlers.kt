package com.wasd.qlic.consumption.application.handlers

import com.wasd.qlic.consumption.application.commands.DefineSustainabilityGoalCommand
import com.wasd.qlic.consumption.application.commands.DefineTariffCommand
import com.wasd.qlic.consumption.application.queries.GetPeriodComparisonQuery
import com.wasd.qlic.consumption.application.queries.GetPeriodReportQuery
import com.wasd.qlic.consumption.application.queries.GetSustainabilityGoalQuery
import com.wasd.qlic.consumption.application.queries.GetTariffQuery
import com.wasd.qlic.consumption.domain.exceptions.GoalBaselineUnavailableException
import com.wasd.qlic.consumption.domain.exceptions.InvalidGoalException
import com.wasd.qlic.consumption.domain.exceptions.InvalidPeriodException
import com.wasd.qlic.consumption.domain.exceptions.InvalidTariffException
import com.wasd.qlic.consumption.domain.exceptions.SustainabilityGoalNotFoundException
import com.wasd.qlic.consumption.domain.model.GoalProgress
import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.PeriodComparison
import com.wasd.qlic.consumption.domain.model.PeriodConsumptionReport
import com.wasd.qlic.consumption.domain.model.SustainabilityGoal
import com.wasd.qlic.consumption.domain.model.TariffSettings
import com.wasd.qlic.consumption.domain.model.WaterPointConsumption
import com.wasd.qlic.consumption.domain.model.WaterTariff
import com.wasd.qlic.consumption.domain.repositories.SustainabilityGoalRepository
import com.wasd.qlic.consumption.domain.repositories.TariffSettingsRepository
import com.wasd.qlic.consumption.domain.services.ConsumptionReadingProvider
import com.wasd.qlic.consumption.domain.services.CostEstimationService
import com.wasd.qlic.consumption.domain.services.PeriodComparisonService
import com.wasd.qlic.consumption.domain.services.RecommendationService
import com.wasd.qlic.consumption.domain.services.ServiceCategoryProvider
import com.wasd.qlic.consumption.domain.services.TariffCatalog
import com.wasd.qlic.consumption.domain.services.WaterPointCatalogProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

/** US23: tariff applied to the account, from its own settings or from the category of its plan. */
@Component
class TariffHandler(
    private val settingsRepository: TariffSettingsRepository,
    private val serviceCategoryProvider: ServiceCategoryProvider,
    private val tariffCatalog: TariffCatalog
) {
    @Transactional(readOnly = true)
    fun handle(query: GetTariffQuery): WaterTariff {
        val settings = settingsRepository.findByAccountId(query.accountId)
        val category = settings?.category ?: serviceCategoryProvider.defaultCategory(query.accountId)
        val custom = settings?.customPricePerCubicMeter
        return WaterTariff(category, custom ?: tariffCatalog.pricePerCubicMeter(category), custom != null)
    }

    @Transactional
    fun handle(command: DefineTariffCommand): WaterTariff {
        val current = settingsRepository.findByAccountId(command.accountId)
        try {
            val settings = current?.also { it.redefine(command.category, command.customPricePerCubicMeter) }
                ?: TariffSettings(command.accountId, command.category, command.customPricePerCubicMeter)
            settingsRepository.save(settings)
        } catch (exception: IllegalArgumentException) {
            throw InvalidTariffException()
        }
        return handle(GetTariffQuery(command.accountId))
    }
}

/** US27: definition and progress of the monthly reduction goal. */
@Component
class SustainabilityGoalHandler(
    private val goalRepository: SustainabilityGoalRepository,
    private val readingProvider: ConsumptionReadingProvider
) {
    @Transactional
    fun handle(command: DefineSustainabilityGoalCommand): GoalProgress {
        val previousMonth = command.month.minusMonths(1)
        val baseline = readingProvider.readings(command.accountId, Period(previousMonth.atDay(1), previousMonth.atEndOfMonth()))
            .sumOf { it.volumeLiters }
        if (baseline.signum() == 0) throw GoalBaselineUnavailableException()
        val current = goalRepository.findByAccountIdAndMonth(command.accountId, command.month)
        val goal = try {
            current?.also { it.redefine(command.reductionPercentage, baseline) }
                ?: SustainabilityGoal(command.accountId, command.month, command.reductionPercentage, baseline)
        } catch (exception: IllegalArgumentException) {
            throw InvalidGoalException()
        }
        return progressOf(goalRepository.save(goal), command.today)
    }

    @Transactional(readOnly = true)
    fun handle(query: GetSustainabilityGoalQuery): GoalProgress {
        val goal = goalRepository.findByAccountIdAndMonth(query.accountId, query.month) ?: throw SustainabilityGoalNotFoundException()
        return progressOf(goal, query.today)
    }

    /** Progress of the goal of the month, if one was defined. */
    @Transactional(readOnly = true)
    fun progressFor(accountId: UUID, month: YearMonth, today: LocalDate): GoalProgress? =
        goalRepository.findByAccountIdAndMonth(accountId, month)?.let { progressOf(it, today) }

    private fun progressOf(goal: SustainabilityGoal, today: LocalDate): GoalProgress {
        val start = goal.month.atDay(1)
        val end = earliest(goal.month.atEndOfMonth(), today)
        val consumed = if (end.isBefore(start)) BigDecimal.ZERO
        else readingProvider.readings(goal.accountId, Period(start, end)).sumOf { it.volumeLiters }
        return goal.progress(consumed, today)
    }
}

/**
 * US21, US22, US23, US25: consumption report of a period with its distribution by Water Point,
 * estimated cost, saving recommendations, goal progress and comparison with the previous period.
 */
@Component
@Transactional(readOnly = true)
class ConsumptionReportQueryHandler(
    private val readingProvider: ConsumptionReadingProvider,
    private val waterPointCatalog: WaterPointCatalogProvider,
    private val tariffHandler: TariffHandler,
    private val goalHandler: SustainabilityGoalHandler,
    private val costEstimationService: CostEstimationService,
    private val comparisonService: PeriodComparisonService,
    private val recommendationService: RecommendationService
) {
    fun handle(query: GetPeriodReportQuery): PeriodConsumptionReport {
        val period = periodOf(query.from ?: query.today.withDayOfMonth(1), query.to ?: query.today)
        val readings = readingProvider.readings(query.accountId, period)
        val tariff = tariffHandler.handle(GetTariffQuery(query.accountId))
        val catalog = waterPointCatalog.waterPoints(query.accountId)
        val total = readings.sumOf { it.volumeLiters }

        val byWaterPoint = readings.groupBy { it.waterPointId }.map { (waterPointId, items) ->
            val liters = items.sumOf { it.volumeLiters }
            WaterPointConsumption(
                waterPointId = waterPointId,
                waterPointName = catalog[waterPointId]?.name,
                waterPointType = catalog[waterPointId]?.type,
                liters = liters.scaled(),
                percentage = if (total.signum() == 0) BigDecimal.ZERO.setScale(2)
                else liters.multiply(BigDecimal(100)).divide(total, 2, RoundingMode.HALF_UP),
                estimatedCost = costEstimationService.estimate(liters, tariff)
            )
        }.sortedByDescending { it.liters }

        val baselinePeriod = Period(
            period.from.minusDays(RecommendationService.BASELINE_DAYS),
            period.from.minusDays(1)
        )
        val recommendations = recommendationService.recommend(
            period, readings, readingProvider.readings(query.accountId, baselinePeriod), catalog
        )
        val month = YearMonth.from(period.from)
        val goal = if (YearMonth.from(period.to) == month) goalHandler.progressFor(query.accountId, month, query.today) else null

        return PeriodConsumptionReport(
            accountId = query.accountId,
            period = period,
            readingsCount = readings.size,
            totalLiters = total.scaled(),
            estimatedCost = costEstimationService.estimate(total, tariff),
            tariff = tariff,
            byWaterPoint = byWaterPoint,
            recommendationsAvailable = recommendations != null,
            recommendations = recommendations.orEmpty(),
            goal = goal
        )
    }

    /**
     * Without dates, the current month up to today is compared with the same days of the previous month.
     * With dates, the period is compared with the previous period of the same length.
     */
    fun handle(query: GetPeriodComparisonQuery): PeriodComparison {
        val (current, previous) = if (query.from == null && query.to == null) {
            val start = query.today.withDayOfMonth(1)
            val previousMonth = YearMonth.from(start).minusMonths(1)
            val days = ChronoUnit.DAYS.between(start, query.today)
            periodOf(start, query.today) to
                periodOf(previousMonth.atDay(1), earliest(previousMonth.atDay(1).plusDays(days), previousMonth.atEndOfMonth()))
        } else {
            val selected = periodOf(query.from ?: query.today.withDayOfMonth(1), query.to ?: query.today)
            val length = ChronoUnit.DAYS.between(selected.from, selected.to) + 1
            selected to periodOf(selected.from.minusDays(length), selected.from.minusDays(1))
        }
        return comparisonService.compare(
            current, readingProvider.readings(query.accountId, current),
            previous, readingProvider.readings(query.accountId, previous)
        )
    }

    private fun periodOf(from: LocalDate, to: LocalDate): Period = try {
        Period(from, to)
    } catch (exception: IllegalArgumentException) {
        throw InvalidPeriodException()
    }

    private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}

private fun earliest(first: LocalDate, second: LocalDate): LocalDate = if (first.isBefore(second)) first else second
