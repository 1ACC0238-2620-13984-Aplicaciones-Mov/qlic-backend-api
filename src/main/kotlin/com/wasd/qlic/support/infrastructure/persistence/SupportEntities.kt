package com.wasd.qlic.support.infrastructure.persistence

import com.wasd.qlic.support.domain.model.IncidentOrigin
import com.wasd.qlic.support.domain.model.IncidentState
import com.wasd.qlic.support.domain.model.VisitState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** Persistence of the Incident aggregate; its technical visit and resolution are stored in the same row. */
@Entity
@Table(name = "incidents")
class IncidentEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "reported_by", nullable = false) val reportedBy: UUID,
    @Column(nullable = false, length = 500) val description: String,
    @Column(name = "device_id") val deviceId: UUID?,
    @Column(name = "water_point_id") val waterPointId: UUID?,
    @Column(name = "alert_id") val alertId: UUID?,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val origin: IncidentOrigin,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var state: IncidentState,
    @Column(name = "registered_at", nullable = false) val registeredAt: Instant,
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant,

    @Column(name = "visit_id") var visitId: UUID? = null,
    @Column(name = "visit_preferred_date") var visitPreferredDate: LocalDate? = null,
    @Column(name = "visit_from_time") var visitFromTime: LocalTime? = null,
    @Column(name = "visit_to_time") var visitToTime: LocalTime? = null,
    @Column(name = "visit_notes", length = 300) var visitNotes: String? = null,
    @Enumerated(EnumType.STRING) @Column(name = "visit_state", length = 20) var visitState: VisitState? = null,
    @Column(name = "visit_technician_id") var visitTechnicianId: UUID? = null,
    @Column(name = "visit_technician_name", length = 120) var visitTechnicianName: String? = null,
    @Column(name = "visit_scheduled_at") var visitScheduledAt: LocalDateTime? = null,
    @Column(name = "visit_requested_at") var visitRequestedAt: Instant? = null,

    @Column(name = "resolution_description", length = 500) var resolutionDescription: String? = null,
    @Column(name = "resolved_at") var resolvedAt: Instant? = null
)

@Entity
@Table(name = "technicians")
class TechnicianEntity(
    @Id val id: UUID,
    @Column(name = "full_name", nullable = false, length = 120) val fullName: String,
    @Column(nullable = false, length = 20) val phone: String,
    @Column(name = "service_zone", nullable = false, length = 80) val zone: String,
    @Column(nullable = false) val active: Boolean
)

@Entity
@Table(name = "terms_of_service")
class TermsOfServiceEntity(
    @Id val id: UUID,
    @Column(name = "document_version", nullable = false, length = 20) val version: String,
    @Column(name = "language_code", nullable = false, length = 10) val language: String,
    @Column(nullable = false, length = 150) val title: String,
    @Column(nullable = false, columnDefinition = "TEXT") val content: String,
    @Column(name = "updated_on", nullable = false) val updatedOn: LocalDate
)
