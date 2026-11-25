package com.wasd.qlic.consumption.interfaces.rest.resources

import com.wasd.qlic.consumption.domain.model.ComparisonTrend
import com.wasd.qlic.consumption.domain.model.GoalStatus
import com.wasd.qlic.consumption.domain.model.ServiceCategory
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Estimated cost in soles. Always referential: the real bill depends on the tiered tariff of the provider (US23).")
data class EstimatedCostResource(
    @field:Schema(example = "64.25") val amount: BigDecimal,
    @field:Schema(example = "PEN") val currency: String,
    @field:Schema(example = "true") val estimate: Boolean = true
)

@Schema(description = "Tariff applied to the estimated cost of the account (US23).")
data class TariffResource(
    val category: ServiceCategory,
    @field:Schema(example = "5.00", description = "Soles per cubic meter, water and sewerage") val pricePerCubicMeter: BigDecimal,
    @field:Schema(description = "True when the account configured its own price") val custom: Boolean
)

@Schema(description = "Service category of the account and, optionally, its own price per cubic meter (US23).")
data class DefineTariffResource(
    @field:NotNull(message = "{validation.tariff.category.required}")
    val category: ServiceCategory?,
    @field:DecimalMin(value = "0.01", message = "{validation.tariff.price.min}")
    @field:Schema(example = "5.40", nullable = true)
    val customPricePerCubicMeter: BigDecimal? = null
)

@Schema(description = "Consumption of a Water Point in the period.")
data class WaterPointConsumptionResource(
    val waterPointId: UUID,
    @field:Schema(nullable = true) val waterPointName: String?,
    @field:Schema(nullable = true, example = "KITCHEN") val waterPointType: String?,
    @field:Schema(example = "820.50") val liters: BigDecimal,
    @field:Schema(example = "42.15") val percentage: BigDecimal,
    val estimatedCost: EstimatedCostResource
)

@Schema(description = "Saving recommendation for one of the Water Points with the highest consumption (US25).")
data class SavingRecommendationResource(
    val waterPointId: UUID,
    @field:Schema(nullable = true) val waterPointName: String?,
    @field:Schema(example = "95.30") val dailyAverageLiters: BigDecimal,
    @field:Schema(example = "80.00") val baselineDailyAverageLiters: BigDecimal,
    @field:Schema(example = "19.13", nullable = true) val increasePercentage: BigDecimal?,
    val message: String
)

@Schema(description = "Progress of the sustainability goal of the month (US27).")
data class SustainabilityGoalResource(
    @field:Schema(example = "2026-11") val month: String,
    @field:Schema(example = "10.00") val reductionPercentage: BigDecimal,
    @field:Schema(example = "4500.00", description = "Consumption of the previous month") val baselineLiters: BigDecimal,
    @field:Schema(example = "4050.00") val targetLiters: BigDecimal,
    @field:Schema(example = "2310.40") val consumedLiters: BigDecimal,
    @field:Schema(example = "57.05") val targetUsedPercentage: BigDecimal,
    @field:Schema(example = "80.00") val monthElapsedPercentage: BigDecimal,
    val status: GoalStatus,
    val message: String
)

@Schema(description = "Reduction goal for a month, in percent of the consumption of the previous month (US27).")
data class DefineSustainabilityGoalResource(
    @field:NotNull(message = "{validation.goal.reduction.required}")
    @field:DecimalMin(value = "1", message = "{validation.goal.reduction.range}")
    @field:DecimalMax(value = "90", message = "{validation.goal.reduction.range}")
    @field:Schema(example = "10")
    val reductionPercentage: BigDecimal?
)

@Schema(description = "Consumption report of a period (US21, US23, US25, US27).")
data class PeriodConsumptionReportResource(
    val from: LocalDate,
    val to: LocalDate,
    @field:Schema(description = "False when there are no readings in the period (US21 - Scenario 2)") val hasData: Boolean,
    @field:Schema(nullable = true, description = "Localized notice when there is no data") val message: String?,
    @field:Schema(example = "1946.30") val totalLiters: BigDecimal,
    @field:Schema(example = "1.95") val totalCubicMeters: BigDecimal,
    val estimatedCost: EstimatedCostResource,
    val tariff: TariffResource,
    val byWaterPoint: List<WaterPointConsumptionResource>,
    @field:Schema(description = "False while there is not enough history for a baseline (7 days in the previous 30)")
    val recommendationsAvailable: Boolean,
    val recommendations: List<SavingRecommendationResource>,
    @field:Schema(nullable = true) val goal: SustainabilityGoalResource?
)

@Schema(description = "Comparison of the period with the previous one (US22).")
data class PeriodComparisonResource(
    val currentFrom: LocalDate,
    val currentTo: LocalDate,
    @field:Schema(example = "1946.30") val currentLiters: BigDecimal,
    val previousFrom: LocalDate,
    val previousTo: LocalDate,
    @field:Schema(example = "1720.00") val previousLiters: BigDecimal,
    @field:Schema(description = "False when one of the periods has no readings (US22 - Scenario 2)") val comparable: Boolean,
    @field:Schema(example = "226.30", nullable = true) val variationLiters: BigDecimal?,
    @field:Schema(example = "13.16", nullable = true) val variationPercentage: BigDecimal?,
    @field:Schema(nullable = true) val trend: ComparisonTrend?,
    val message: String
)

@Schema(description = "Summary of the report ready to be shared through the share options of the device (US26).")
data class ShareableReportResource(
    val title: String,
    val text: String
)
