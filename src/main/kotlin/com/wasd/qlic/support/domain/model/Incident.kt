package com.wasd.qlic.support.domain.model

import com.wasd.qlic.support.domain.exceptions.InvalidIncidentTransitionException
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/**
 * Aggregate Root: case registered by a subscriber when a device fails or a leak is confirmed (US35),
 * with its attention cycle: technical visit (US36) and follow-up (US37).
 */
class Incident(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val reportedBy: UUID,
    val description: String,
    val deviceId: UUID? = null,
    val waterPointId: UUID? = null,
    val alertId: UUID? = null,
    val origin: IncidentOrigin = if (alertId != null) IncidentOrigin.ALERT_DERIVED else IncidentOrigin.SELF_REPORTED,
    var state: IncidentState = IncidentState.PENDING,
    var technicalVisit: TechnicalVisit? = null,
    var resolution: Resolution? = null,
    val registeredAt: Instant = Instant.now(),
    var updatedAt: Instant = registeredAt
) {
    init {
        require(description.isNotBlank()) { "The incident description is required" }
    }

    /** US36 - Scenario 1: the visit can only be requested for a PENDING incident. */
    fun requestTechnicalVisit(availability: AvailabilityWindow, notes: String?): TechnicalVisit {
        transitionTo(IncidentState.VISIT_REQUESTED)
        val visit = TechnicalVisit(availability = availability, notes = notes?.trim()?.takeIf { it.isNotEmpty() })
        technicalVisit = visit
        return visit
    }

    /** US36 - Scenario 2: assigning a technician puts the incident IN_PROGRESS. */
    fun assignTechnician(technician: Technician, scheduledAt: LocalDateTime): TechnicalVisit {
        val visit = technicalVisit ?: throw InvalidIncidentTransitionException(state.name, IncidentState.IN_PROGRESS.name)
        transitionTo(IncidentState.IN_PROGRESS)
        visit.assign(technician, scheduledAt)
        return visit
    }

    fun resolve(resolution: Resolution) {
        transitionTo(IncidentState.RESOLVED)
        this.resolution = resolution
        technicalVisit?.complete()
        technicalVisit?.cancel()
    }

    private fun transitionTo(target: IncidentState) {
        if (!state.canTransitionTo(target)) throw InvalidIncidentTransitionException(state.name, target.name)
        state = target
        updatedAt = Instant.now()
    }
}
