package com.wasd.qlic.support.infrastructure.persistence

import com.wasd.qlic.support.domain.model.AvailabilityWindow
import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.domain.model.IncidentState
import com.wasd.qlic.support.domain.model.Resolution
import com.wasd.qlic.support.domain.model.TechnicalVisit
import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.model.TermsOfService
import com.wasd.qlic.support.domain.repositories.IncidentRepository
import com.wasd.qlic.support.domain.repositories.TechnicianRepository
import com.wasd.qlic.support.domain.repositories.TermsOfServiceRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataIncidentRepository : JpaRepository<IncidentEntity, UUID> {
    fun findAllByAccountId(accountId: UUID): List<IncidentEntity>
    fun findAllByState(state: IncidentState): List<IncidentEntity>
}

@Repository
interface SpringDataTechnicianRepository : JpaRepository<TechnicianEntity, UUID> {
    fun findAllByActiveTrue(): List<TechnicianEntity>
}

@Repository
interface SpringDataTermsOfServiceRepository : JpaRepository<TermsOfServiceEntity, UUID> {
    fun findFirstByLanguageOrderByUpdatedOnDesc(language: String): TermsOfServiceEntity?
}

@Component
class JpaIncidentRepositoryImpl(
    private val jpa: SpringDataIncidentRepository
) : IncidentRepository {

    override fun save(incident: Incident): Incident = toDomain(jpa.save(toEntity(incident)))

    override fun findById(id: UUID): Incident? = jpa.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<Incident> = jpa.findAllByAccountId(accountId).map { toDomain(it) }

    override fun findByState(state: IncidentState): List<Incident> = jpa.findAllByState(state).map { toDomain(it) }

    private fun toEntity(incident: Incident): IncidentEntity {
        val visit = incident.technicalVisit
        return IncidentEntity(
            id = incident.id,
            accountId = incident.accountId,
            reportedBy = incident.reportedBy,
            description = incident.description,
            deviceId = incident.deviceId,
            waterPointId = incident.waterPointId,
            alertId = incident.alertId,
            origin = incident.origin,
            state = incident.state,
            registeredAt = incident.registeredAt,
            updatedAt = incident.updatedAt,
            visitId = visit?.id,
            visitPreferredDate = visit?.availability?.preferredDate,
            visitFromTime = visit?.availability?.fromTime,
            visitToTime = visit?.availability?.toTime,
            visitNotes = visit?.notes,
            visitState = visit?.state,
            visitTechnicianId = visit?.technicianId,
            visitTechnicianName = visit?.technicianName,
            visitScheduledAt = visit?.scheduledAt,
            visitRequestedAt = visit?.requestedAt,
            resolutionDescription = incident.resolution?.description,
            resolvedAt = incident.resolution?.resolvedAt
        )
    }

    private fun toDomain(entity: IncidentEntity): Incident {
        val visit = if (entity.visitId != null && entity.visitPreferredDate != null &&
            entity.visitFromTime != null && entity.visitToTime != null
        ) {
            TechnicalVisit(
                id = entity.visitId!!,
                availability = AvailabilityWindow(entity.visitPreferredDate!!, entity.visitFromTime!!, entity.visitToTime!!),
                notes = entity.visitNotes,
                state = entity.visitState!!,
                technicianId = entity.visitTechnicianId,
                technicianName = entity.visitTechnicianName,
                scheduledAt = entity.visitScheduledAt,
                requestedAt = entity.visitRequestedAt!!
            )
        } else null
        val resolution = entity.resolutionDescription?.let { Resolution(it, entity.resolvedAt!!) }
        return Incident(
            id = entity.id,
            accountId = entity.accountId,
            reportedBy = entity.reportedBy,
            description = entity.description,
            deviceId = entity.deviceId,
            waterPointId = entity.waterPointId,
            alertId = entity.alertId,
            origin = entity.origin,
            state = entity.state,
            technicalVisit = visit,
            resolution = resolution,
            registeredAt = entity.registeredAt,
            updatedAt = entity.updatedAt
        )
    }
}

@Component
class JpaTechnicianRepositoryImpl(
    private val jpa: SpringDataTechnicianRepository
) : TechnicianRepository {

    override fun save(technician: Technician): Technician = toDomain(
        jpa.save(TechnicianEntity(technician.id, technician.fullName, technician.phone, technician.zone, technician.active))
    )

    override fun findById(id: UUID): Technician? = jpa.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findAllActive(): List<Technician> = jpa.findAllByActiveTrue().map { toDomain(it) }

    override fun count(): Long = jpa.count()

    private fun toDomain(entity: TechnicianEntity) =
        Technician(entity.id, entity.fullName, entity.phone, entity.zone, entity.active)
}

@Component
class JpaTermsOfServiceRepositoryImpl(
    private val jpa: SpringDataTermsOfServiceRepository
) : TermsOfServiceRepository {

    override fun save(terms: TermsOfService): TermsOfService = toDomain(
        jpa.save(TermsOfServiceEntity(terms.id, terms.version, terms.language, terms.title, terms.content, terms.updatedOn))
    )

    override fun findLatestByLanguage(language: String): TermsOfService? =
        jpa.findFirstByLanguageOrderByUpdatedOnDesc(language)?.let { toDomain(it) }

    override fun count(): Long = jpa.count()

    private fun toDomain(entity: TermsOfServiceEntity) =
        TermsOfService(entity.id, entity.version, entity.language, entity.title, entity.content, entity.updatedOn)
}
