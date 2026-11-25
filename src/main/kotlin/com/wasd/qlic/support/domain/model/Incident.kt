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
    var updatedAt: Instant = registeredAt,
    statusHistory: List<IncidentStatusChange> = listOf(IncidentStatusChange(fromState = null, toState = state, occurredAt = registeredAt))
) {
    private val statusChanges: MutableList<IncidentStatusChange> = statusHistory.toMutableList()

    /** US37: every state the incident went through, oldest first. */
    val history: List<IncidentStatusChange> get() = statusChanges.sortedBy { it.occurredAt }

    init {
        require(description.isNotBlank()) { "The incident description is required" }
    }

    /** US36 - Scenario 1: the visit can only be requested for a PENDING incident. */
    fun requestTechnicalVisit(availability: AvailabilityWindow, notes: String?): TechnicalVisit {
        transitionTo(IncidentState.VISIT_REQUESTED, availability.preferredDate.toString())
        val visit = TechnicalVisit(availability = availability, notes = notes?.trim()?.takeIf { it.isNotEmpty() })
        technicalVisit = visit
        return visit
    }

    /** US36 - Scenario 2: assigning a technician puts the incident IN_PROGRESS. */
    fun assignTechnician(technician: Technician, scheduledAt: LocalDateTime): TechnicalVisit {
        val visit = technicalVisit ?: throw InvalidIncidentTransitionException(state.name, IncidentState.IN_PROGRESS.name)
        transitionTo(IncidentState.IN_PROGRESS, "${technician.fullName} - $scheduledAt")
        visit.assign(technician, scheduledAt)
        return visit
    }

    fun resolve(resolution: Resolution) {
        transitionTo(IncidentState.RESOLVED, resolution.description)
        this.resolution = resolution
        technicalVisit?.complete()
        technicalVisit?.cancel()
    }

    private fun transitionTo(target: IncidentState, detail: String?) {
        if (!state.canTransitionTo(target)) throw InvalidIncidentTransitionException(state.name, target.name)
        val now = Instant.now()
        statusChanges.add(IncidentStatusChange(fromState = state, toState = target, occurredAt = now, detail = detail))
        state = target
        updatedAt = now
    }
}

/**
 * Entity: one step of the attention cycle of an incident (US37). The detail is the preferred visit date,
 * the technician and scheduled date, or the resolution, depending on the new state.
 */
data class IncidentStatusChange(
    val id: UUID = UUID.randomUUID(),
    val fromState: IncidentState?,
    val toState: IncidentState,
    val occurredAt: Instant,
    val detail: String? = null
)
