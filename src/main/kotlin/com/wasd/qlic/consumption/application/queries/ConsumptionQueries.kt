package com.wasd.qlic.consumption.application.queries

import java.time.LocalDate
import java.util.UUID

/** US24: consumption report of a period differentiated by activity hours. */
data class GetActivityConsumptionReportQuery(val accountId: UUID, val from: LocalDate, val to: LocalDate)

/** US24: activity schedule declared by an account. */
data class GetActivityScheduleQuery(val accountId: UUID)

/** US21, US23, US25, US27: consumption report of a period; without dates, the current month up to today. */
data class GetPeriodReportQuery(
    val accountId: UUID,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val today: LocalDate = LocalDate.now()
)

/** US22: comparison of a period with the previous one. */
data class GetPeriodComparisonQuery(
    val accountId: UUID,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val today: LocalDate = LocalDate.now()
)

/** US23: tariff applied to the estimated cost of the account. */
data class GetTariffQuery(val accountId: UUID)

/** US27: progress of the sustainability goal of a month. */
data class GetSustainabilityGoalQuery(
    val accountId: UUID,
    val month: java.time.YearMonth,
    val today: LocalDate = LocalDate.now()
)
