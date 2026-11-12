package com.wasd.qlic.support.application.handlers

import com.wasd.qlic.support.application.queries.GetIncidentByIdQuery
import com.wasd.qlic.support.application.queries.GetIncidentsByAccountQuery
import com.wasd.qlic.support.application.queries.GetTermsOfServiceQuery
import com.wasd.qlic.support.domain.exceptions.IncidentNotFoundException
import com.wasd.qlic.support.domain.exceptions.TermsOfServiceNotFoundException
import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.model.TermsOfService
import com.wasd.qlic.support.domain.repositories.IncidentRepository
import com.wasd.qlic.support.domain.repositories.TechnicianRepository
import com.wasd.qlic.support.domain.repositories.TermsOfServiceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Read side of Support: incidents (US37), technicians and terms of service (US38). */
@Component
@Transactional(readOnly = true)
class SupportQueryHandler(
    private val incidentRepository: IncidentRepository,
    private val technicianRepository: TechnicianRepository,
    private val termsOfServiceRepository: TermsOfServiceRepository
) {
    /** US37 - Scenario 1: incidents with registration date, device and current state, newest first. */
    fun handle(query: GetIncidentsByAccountQuery): List<Incident> =
        incidentRepository.findByAccountId(query.accountId).sortedByDescending { it.registeredAt }

    fun handle(query: GetIncidentByIdQuery): Incident =
        incidentRepository.findById(query.incidentId)?.takeIf { it.accountId == query.accountId }
            ?: throw IncidentNotFoundException()

    /** US38 - Scenario 1: current document; English is the fallback language. */
    fun handle(query: GetTermsOfServiceQuery): TermsOfService =
        termsOfServiceRepository.findLatestByLanguage(query.language)
            ?: termsOfServiceRepository.findLatestByLanguage(DEFAULT_LANGUAGE)
            ?: throw TermsOfServiceNotFoundException()

    fun activeTechnicians(): List<Technician> = technicianRepository.findAllActive()

    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }
}
