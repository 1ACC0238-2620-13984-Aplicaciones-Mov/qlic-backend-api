package com.wasd.qlic.support.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Value Object: time window declared by the subscriber to receive the technical visit (US36). */
data class AvailabilityWindow(
    val preferredDate: LocalDate,
    val fromTime: LocalTime,
    val toTime: LocalTime
) {
    init {
        require(fromTime.isBefore(toTime)) { "The availability window must start before it ends" }
    }
}

/** Value Object: description of the applied solution and its registration date. */
data class Resolution(
    val description: String,
    val resolvedAt: Instant = Instant.now()
) {
    init {
        require(description.isNotBlank()) { "The resolution description is required" }
    }
}
