package com.wasd.qlic.support.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity: visit scheduled to attend an incident at the household or premises of the subscriber (US36).
 */
class TechnicalVisit(
    val id: UUID = UUID.randomUUID(),
    val availability: AvailabilityWindow,
    val notes: String? = null,
    var state: VisitState = VisitState.REQUESTED,
    var technicianId: UUID? = null,
    var technicianName: String? = null,
    var scheduledAt: LocalDateTime? = null,
    val requestedAt: Instant = Instant.now()
) {
    /** US36 - Scenario 2: a technician is assigned with the scheduled date. */
    fun assign(technician: Technician, scheduledAt: LocalDateTime) {
        this.technicianId = technician.id
        this.technicianName = technician.fullName
        this.scheduledAt = scheduledAt
        this.state = VisitState.ASSIGNED
    }

    fun complete() {
        if (state == VisitState.ASSIGNED) state = VisitState.COMPLETED
    }

    fun cancel() {
        if (state == VisitState.REQUESTED) state = VisitState.CANCELLED
    }
}
