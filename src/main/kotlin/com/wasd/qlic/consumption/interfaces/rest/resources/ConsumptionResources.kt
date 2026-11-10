package com.wasd.qlic.consumption.interfaces.rest.resources

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Schema(description = "Activity schedule of the premises (US24). Closing before opening means the schedule crosses midnight.")
data class ActivityScheduleResource(
    @field:NotNull(message = "{validation.schedule.time.required}")
    @field:Schema(type = "string", example = "08:00")
    val openingTime: LocalTime?,

    @field:NotNull(message = "{validation.schedule.time.required}")
    @field:Schema(type = "string", example = "20:00")
    val closingTime: LocalTime?,

    @field:NotEmpty(message = "{validation.schedule.days.required}")
    @field:ArraySchema(schema = Schema(example = "MONDAY"))
    val activeDays: Set<DayOfWeek>?,

    @field:Schema(accessMode = Schema.AccessMode.READ_ONLY, nullable = true)
    val updatedAt: Instant? = null
)

@Schema(description = "Consumption of a Water Point inside and outside the activity hours.")
data class WaterPointActivityConsumptionResource(
    val waterPointId: UUID,
    @field:Schema(example = "320.50") val totalLiters: BigDecimal,
    @field:Schema(example = "250.00", nullable = true) val withinActivityLiters: BigDecimal?,
    @field:Schema(example = "70.50", nullable = true) val outsideActivityLiters: BigDecimal?
)

@Schema(description = "Consumption report of the period differentiated by activity hours (US24 - Scenario 1).")
data class ActivityConsumptionReportResource(
    val from: LocalDate,
    val to: LocalDate,
    @field:Schema(description = "False when the premises has not declared its schedule; then only totals are returned")
    val scheduleDefined: Boolean,
    val schedule: ActivityScheduleResource?,
    @field:Schema(example = "1250.75") val totalLiters: BigDecimal,
    @field:Schema(example = "1010.25", nullable = true) val withinActivityLiters: BigDecimal?,
    @field:Schema(example = "240.50", nullable = true) val outsideActivityLiters: BigDecimal?,
    @field:Schema(example = "19.23", nullable = true) val outsideActivityPercentage: BigDecimal?,
    val byWaterPoint: List<WaterPointActivityConsumptionResource>
)
