package com.wasd.qlic.support.domain.services

import com.wasd.qlic.support.domain.exceptions.NoTechnicianAvailableException
import com.wasd.qlic.support.domain.exceptions.TechnicianNotFoundException
import com.wasd.qlic.support.domain.model.IncidentState
import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.repositories.IncidentRepository
import com.wasd.qlic.support.domain.repositories.TechnicianRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Domain Service: determines the technician to assign (US36 - Scenario 2). When support staff does
 * not choose one, the active technician with the fewest visits in progress is selected.
 */
@Service
class VisitAssignmentService(
    private val technicianRepository: TechnicianRepository,
    private val incidentRepository: IncidentRepository
) {
    fun selectTechnician(requestedTechnicianId: UUID?): Technician {
        if (requestedTechnicianId != null) {
            return technicianRepository.findById(requestedTechnicianId)?.takeIf { it.active }
                ?: throw TechnicianNotFoundException()
        }
        val workload = incidentRepository.findByState(IncidentState.IN_PROGRESS)
            .mapNotNull { it.technicalVisit?.technicianId }
            .groupingBy { it }
            .eachCount()
        return technicianRepository.findAllActive().minByOrNull { workload[it.id] ?: 0 }
            ?: throw NoTechnicianAvailableException()
    }
}
