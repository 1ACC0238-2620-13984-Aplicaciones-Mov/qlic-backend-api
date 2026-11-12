package com.wasd.qlic.support.domain.repositories

import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.domain.model.IncidentState
import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.model.TermsOfService
import java.util.UUID

interface IncidentRepository {
    fun save(incident: Incident): Incident
    fun findById(id: UUID): Incident?
    fun findByAccountId(accountId: UUID): List<Incident>
    fun findByState(state: IncidentState): List<Incident>
}

interface TechnicianRepository {
    fun save(technician: Technician): Technician
    fun findById(id: UUID): Technician?
    fun findAllActive(): List<Technician>
    fun count(): Long
}

interface TermsOfServiceRepository {
    fun save(terms: TermsOfService): TermsOfService
    fun findLatestByLanguage(language: String): TermsOfService?
    fun count(): Long
}
