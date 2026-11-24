package com.wasd.qlic.consumption.domain.model

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Value Object: interval of days over which the consumption is aggregated. */
data class Period(val from: LocalDate, val to: LocalDate) {
    init {
        require(!to.isBefore(from)) { "The end of the period cannot be before its start" }
        require(ChronoUnit.DAYS.between(from, to) <= MAX_DAYS) { "The period cannot exceed $MAX_DAYS days" }
    }

    fun start(): LocalDateTime = from.atStartOfDay()

    fun end(): LocalDateTime = to.atTime(LocalTime.MAX)

    companion object {
        const val MAX_DAYS = 366L
    }
}

/** Reading data that Consumption Analytics needs, translated from Device Monitoring. */
data class ReadingSnapshot(
    val waterPointId: UUID,
    val volumeLiters: BigDecimal,
    val recordedAt: LocalDateTime
)

/** Volume consumed by a Water Point, split by activity hours when the schedule is declared. */
data class WaterPointActivityConsumption(
    val waterPointId: UUID,
    val totalLiters: BigDecimal,
    val withinActivityLiters: BigDecimal?,
    val outsideActivityLiters: BigDecimal?
)

/**
 * Value Object: consumption of the period differentiated inside and outside the activity schedule
 * (US24 - Scenario 1). Without a declared schedule only the total is reported.
 */
data class ActivityConsumptionReport(
    val accountId: UUID,
    val period: Period,
    val schedule: ActivitySchedule?,
    val totalLiters: BigDecimal,
    val withinActivityLiters: BigDecimal?,
    val outsideActivityLiters: BigDecimal?,
    val outsideActivityPercentage: BigDecimal?,
    val byWaterPoint: List<WaterPointActivityConsumption>
) {
    val scheduleDefined: Boolean get() = schedule != null
}
