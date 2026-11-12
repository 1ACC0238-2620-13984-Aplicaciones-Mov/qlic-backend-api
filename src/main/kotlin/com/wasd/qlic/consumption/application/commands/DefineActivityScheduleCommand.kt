package com.wasd.qlic.consumption.application.commands

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/** US24: declare the activity schedule of the premises of an account. */
data class DefineActivityScheduleCommand(
    val accountId: UUID,
    val openingTime: LocalTime,
    val closingTime: LocalTime,
    val activeDays: Set<DayOfWeek>
)
