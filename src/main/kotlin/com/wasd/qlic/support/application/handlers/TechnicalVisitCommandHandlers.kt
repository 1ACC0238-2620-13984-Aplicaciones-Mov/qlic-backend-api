package com.wasd.qlic.support.application.handlers

import com.wasd.qlic.support.application.commands.AssignTechnicianCommand
import com.wasd.qlic.support.application.commands.RequestTechnicalVisitCommand
import com.wasd.qlic.support.application.commands.ResolveIncidentCommand
import com.wasd.qlic.support.domain.exceptions.IncidentNotFoundException
import com.wasd.qlic.support.domain.exceptions.TechnicalVisitNotCoveredException
import com.wasd.qlic.support.domain.model.AvailabilityWindow
import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.domain.model.IncidentResolvedEvent
import com.wasd.qlic.support.domain.model.Resolution
import com.wasd.qlic.support.domain.model.TechnicianAssignedEvent
import com.wasd.qlic.support.domain.repositories.IncidentRepository
import com.wasd.qlic.support.domain.services.PlanEntitlementProvider
import com.wasd.qlic.support.domain.services.SupportNotificationSender
import com.wasd.qlic.support.domain.services.VisitAssignmentService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US36 - Scenario 1: verifies the plan coverage, registers the request and moves the incident
 * to VISIT_REQUESTED.
 */
@Component
class RequestTechnicalVisitCommandHandler(
    private val incidentRepository: IncidentRepository,
    private val planEntitlementProvider: PlanEntitlementProvider
) {
    @Transactional
    fun handle(command: RequestTechnicalVisitCommand): Incident {
        val incident = incidentRepository.findById(command.incidentId)
            ?.takeIf { it.accountId == command.accountId }
            ?: throw IncidentNotFoundException()
        if (!planEntitlementProvider.coversTechnicalVisits(command.accountId)) throw TechnicalVisitNotCoveredException()

        incident.requestTechnicalVisit(
            AvailabilityWindow(command.preferredDate, command.fromTime, command.toTime),
            command.notes
        )
        return incidentRepository.save(incident)
    }
}

/**
 * US36 - Scenario 2: invokes VisitAssignmentService, registers the assignment, notifies the
 * subscriber and publishes TechnicianAssignedEvent.
 */
@Component
class AssignTechnicianCommandHandler(
    private val incidentRepository: IncidentRepository,
    private val visitAssignmentService: VisitAssignmentService,
    private val notificationSender: SupportNotificationSender,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: AssignTechnicianCommand): Incident {
        val incident = incidentRepository.findById(command.incidentId) ?: throw IncidentNotFoundException()
        val technician = visitAssignmentService.selectTechnician(command.technicianId)

        incident.assignTechnician(technician, command.scheduledAt)
        val saved = incidentRepository.save(incident)

        notificationSender.notifyTechnicianAssigned(saved.accountId, saved.id, technician.fullName, command.scheduledAt)
        eventPublisher.publishEvent(
            TechnicianAssignedEvent(
                incidentId = saved.id,
                accountId = saved.accountId,
                technicianId = technician.id,
                scheduledAt = command.scheduledAt
            )
        )
        return saved
    }
}

/** Registers the Resolution, moves the incident to RESOLVED and publishes IncidentResolvedEvent. */
@Component
class ResolveIncidentCommandHandler(
    private val incidentRepository: IncidentRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: ResolveIncidentCommand): Incident {
        val incident = incidentRepository.findById(command.incidentId) ?: throw IncidentNotFoundException()
        incident.resolve(Resolution(command.description.trim()))
        val saved = incidentRepository.save(incident)
        eventPublisher.publishEvent(IncidentResolvedEvent(saved.id, saved.accountId, saved.alertId))
        return saved
    }
}
