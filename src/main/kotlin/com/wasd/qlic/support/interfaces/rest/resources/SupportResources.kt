package com.wasd.qlic.support.interfaces.rest.resources

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Schema(description = "Incident to register (US35). Select the affected device or Water Point, or the active alert that originates it.")
data class RegisterIncidentResource(
    @field:NotBlank(message = "{validation.incident.description.required}")
    @field:Size(max = 500, message = "{validation.incident.description.size}")
    @field:Schema(example = "The sensor of the kitchen sink stopped sending readings since yesterday.")
    val description: String,

    @field:Schema(nullable = true) val deviceId: UUID? = null,
    @field:Schema(nullable = true) val waterPointId: UUID? = null,
    @field:Schema(description = "Active alert from which the incident is registered (US35 - Scenario 2)", nullable = true)
    val alertId: UUID? = null
)

@Schema(description = "Availability of the subscriber to receive the technical visit (US36 - Scenario 1).")
data class RequestTechnicalVisitResource(
    @field:NotNull(message = "{validation.visit.date.required}")
    @field:FutureOrPresent(message = "{validation.visit.date.future}")
    @field:Schema(example = "2026-11-20")
    val preferredDate: LocalDate?,

    @field:NotNull(message = "{validation.visit.time.required}")
    @field:Schema(type = "string", example = "09:00")
    val fromTime: LocalTime?,

    @field:NotNull(message = "{validation.visit.time.required}")
    @field:Schema(type = "string", example = "13:00")
    val toTime: LocalTime?,

    @field:Size(max = 300, message = "{validation.visit.notes.size}")
    @field:Schema(example = "Ring the bell of the store; ask for Claudia.", nullable = true)
    val notes: String? = null
)

@Schema(description = "Assignment of a technician by support staff (US36 - Scenario 2).")
data class AssignTechnicianResource(
    @field:Schema(description = "Optional. When omitted, the technician with the lowest workload is assigned.", nullable = true)
    val technicianId: UUID? = null,

    @field:NotNull(message = "{validation.visit.scheduled-at.required}")
    @field:Schema(example = "2026-11-20T10:00:00")
    val scheduledAt: LocalDateTime?
)

@Schema(description = "Solution applied to the incident.")
data class ResolveIncidentResource(
    @field:NotBlank(message = "{validation.resolution.required}")
    @field:Size(max = 500, message = "{validation.incident.description.size}")
    @field:Schema(example = "The sensor battery was replaced and the readings were restored.")
    val description: String
)

@Schema(description = "Technical visit of an incident (US36).")
data class TechnicalVisitResource(
    val id: UUID,
    val preferredDate: LocalDate,
    @field:Schema(type = "string", example = "09:00") val fromTime: LocalTime,
    @field:Schema(type = "string", example = "13:00") val toTime: LocalTime,
    val notes: String?,
    @field:Schema(example = "ASSIGNED") val state: String,
    val technicianId: UUID?,
    @field:Schema(example = "Miguel Torres") val technicianName: String?,
    val scheduledAt: LocalDateTime?,
    val requestedAt: Instant
)

@Schema(description = "Incident with its registration date, associated device and current state (US37).")
data class IncidentResource(
    val id: UUID,
    val accountId: UUID,
    val description: String,
    val deviceId: UUID?,
    val waterPointId: UUID?,
    val alertId: UUID?,
    @field:Schema(example = "SELF_REPORTED") val origin: String,
    @field:Schema(description = "PENDING, VISIT_REQUESTED, IN_PROGRESS or RESOLVED", example = "PENDING") val state: String,
    val registeredAt: Instant,
    val updatedAt: Instant,
    val technicalVisit: TechnicalVisitResource?,
    val resolution: String?,
    val resolvedAt: Instant?
)

@Schema(description = "Support technician (US36).")
data class TechnicianResource(
    val id: UUID,
    @field:Schema(example = "Miguel Torres") val fullName: String,
    @field:Schema(example = "+51987000111") val phone: String,
    @field:Schema(example = "Lima Centro") val zone: String
)

@Schema(description = "Current terms of service and personal data policy (US38).")
data class TermsOfServiceResource(
    @field:Schema(example = "1.0") val version: String,
    @field:Schema(example = "es") val language: String,
    val title: String,
    val content: String,
    @field:Schema(description = "Date of the last update", example = "2026-11-10") val updatedOn: LocalDate
)
