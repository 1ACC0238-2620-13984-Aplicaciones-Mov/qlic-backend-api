package com.wasd.qlic.consumption.domain.model

import java.time.Instant
import java.util.UUID

/** Activity schedule currently declared by an account for its premises (US24). */
class PremisesActivitySchedule(
    val accountId: UUID,
    var schedule: ActivitySchedule,
    var updatedAt: Instant = Instant.now()
) {
    fun redefine(newSchedule: ActivitySchedule) {
        schedule = newSchedule
        updatedAt = Instant.now()
    }
}
