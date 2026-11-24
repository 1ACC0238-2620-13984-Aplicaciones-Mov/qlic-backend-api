package com.wasd.qlic.support.application.commands

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** US35: register an incident over a device, a Water Point or an active alert. */
data class RegisterIncidentCommand(
    val accountId: UUID,
    val reportedBy: UUID,
    val description: String,
    val deviceId: UUID? = null,
    val waterPointId: UUID? = null,
    val alertId: UUID? = null
)

/** US36 - Scenario 1: request a technical visit for a pending incident. */
data class RequestTechnicalVisitCommand(
    val incidentId: UUID,
    val accountId: UUID,
    val preferredDate: LocalDate,
    val fromTime: LocalTime,
    val toTime: LocalTime,
    val notes: String? = null
)

/** US36 - Scenario 2: support staff assigns a technician to a requested visit. */
data class AssignTechnicianCommand(
    val incidentId: UUID,
    val technicianId: UUID?,
    val scheduledAt: LocalDateTime
)

/** Support staff registers the solution applied to the incident. */
data class ResolveIncidentCommand(
    val incidentId: UUID,
    val description: String
)
