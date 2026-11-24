package com.wasd.qlic.consumption.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Value Object: activity schedule declared for the premises (US24). It supports schedules that
 * cross midnight (for example 18:00 to 02:00); those hours belong to the day the shift started.
 */
data class ActivitySchedule(
    val openingTime: LocalTime,
    val closingTime: LocalTime,
    val activeDays: Set<DayOfWeek>
) {
    init {
        require(openingTime != closingTime) { "Opening and closing times must be different" }
        require(activeDays.isNotEmpty()) { "At least one active day is required" }
    }

    val crossesMidnight: Boolean get() = closingTime.isBefore(openingTime)

    fun isWithinActivity(moment: LocalDateTime): Boolean {
        val time = moment.toLocalTime()
        val day = moment.dayOfWeek
        return if (!crossesMidnight) {
            day in activeDays && !time.isBefore(openingTime) && time.isBefore(closingTime)
        } else {
            (day in activeDays && !time.isBefore(openingTime)) ||
                (day.minus(1) in activeDays && time.isBefore(closingTime))
        }
    }
}
