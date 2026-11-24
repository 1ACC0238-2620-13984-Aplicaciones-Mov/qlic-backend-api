package com.wasd.qlic.consumption.application.queries

import java.time.LocalDate
import java.util.UUID

/** US24: consumption report of a period differentiated by activity hours. */
data class GetActivityConsumptionReportQuery(val accountId: UUID, val from: LocalDate, val to: LocalDate)

/** US24: activity schedule declared by an account. */
data class GetActivityScheduleQuery(val accountId: UUID)
