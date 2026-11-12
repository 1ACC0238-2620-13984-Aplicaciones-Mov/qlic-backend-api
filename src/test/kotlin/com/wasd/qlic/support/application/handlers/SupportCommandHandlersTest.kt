package com.wasd.qlic.support.application.handlers

import com.wasd.qlic.support.application.commands.AssignTechnicianCommand
import com.wasd.qlic.support.application.commands.RegisterIncidentCommand
import com.wasd.qlic.support.application.commands.RequestTechnicalVisitCommand
import com.wasd.qlic.support.application.commands.ResolveIncidentCommand
import com.wasd.qlic.support.application.queries.GetIncidentsByAccountQuery
import com.wasd.qlic.support.domain.exceptions.AlertNotActiveException
import com.wasd.qlic.support.domain.exceptions.DeviceNotFoundForAccountException
import com.wasd.qlic.support.domain.exceptions.IncidentAssetRequiredException
import com.wasd.qlic.support.domain.exceptions.InvalidIncidentTransitionException
import com.wasd.qlic.support.domain.exceptions.TechnicalVisitNotCoveredException
import com.wasd.qlic.support.domain.model.IncidentOrigin
import com.wasd.qlic.support.domain.model.IncidentState
import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.model.VisitState
import com.wasd.qlic.support.domain.repositories.TermsOfServiceRepository
import com.wasd.qlic.support.domain.services.AlertData
import com.wasd.qlic.support.domain.services.DeviceData
import com.wasd.qlic.support.domain.services.VisitAssignmentService
import com.wasd.qlic.support.domain.model.TermsOfService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SupportCommandHandlersTest {

    private val accountId = UUID.randomUUID()
    private val subscriberId = UUID.randomUUID()
    private val deviceId = UUID.randomUUID()
    private val waterPointId = UUID.randomUUID()
    private val activeAlertId = UUID.randomUUID()
    private val acknowledgedAlertId = UUID.randomUUID()

    private val incidents = InMemoryIncidentRepository()
    private val technicians = InMemoryTechnicianRepository()
    private val coverage = FixedCoverageProvider(covered = true)
    private val notifications = RecordingSupportNotificationSender()
    private val noEvents = ApplicationEventPublisher { }

    private lateinit var registerHandler: RegisterIncidentCommandHandler
    private lateinit var requestVisitHandler: RequestTechnicalVisitCommandHandler
    private lateinit var assignHandler: AssignTechnicianCommandHandler
    private lateinit var technician: Technician

    @BeforeEach
    fun setUp() {
        technician = technicians.save(Technician(fullName = "Miguel Torres", phone = "+51987000111", zone = "Lima Centro"))
        registerHandler = RegisterIncidentCommandHandler(
            incidents,
            FakeAlertDataProvider(
                mapOf(
                    activeAlertId to (accountId to AlertData(activeAlertId, waterPointId, active = true)),
                    acknowledgedAlertId to (accountId to AlertData(acknowledgedAlertId, waterPointId, active = false))
                )
            ),
            FakeDeviceDataProvider(mapOf(deviceId to (accountId to DeviceData(deviceId, waterPointId)))),
            noEvents
        )
        requestVisitHandler = RequestTechnicalVisitCommandHandler(incidents, coverage)
        assignHandler = AssignTechnicianCommandHandler(
            incidents, VisitAssignmentService(technicians, incidents), notifications, noEvents
        )
    }

    private fun registerOnDevice() = registerHandler.handle(
        RegisterIncidentCommand(accountId, subscriberId, "The sensor stopped sending readings", deviceId = deviceId)
    )

    private fun requestVisit(incidentId: UUID) = requestVisitHandler.handle(
        RequestTechnicalVisitCommand(
            incidentId, accountId, LocalDate.now().plusDays(3), LocalTime.of(9, 0), LocalTime.of(13, 0), "Ask for Claudia"
        )
    )

    @Test
    @DisplayName("US35 - Scenario 1: the incident is created in PENDING state for the selected device")
    fun shouldRegisterSelfReportedIncident() {
        val incident = registerOnDevice()

        assertEquals(IncidentState.PENDING, incident.state)
        assertEquals(IncidentOrigin.SELF_REPORTED, incident.origin)
        assertEquals(waterPointId, incident.waterPointId)
    }

    @Test
    @DisplayName("US35 - Scenario 1: the device or Water Point is mandatory and must belong to the account")
    fun shouldValidateAffectedAsset() {
        assertThrows<IncidentAssetRequiredException> {
            registerHandler.handle(RegisterIncidentCommand(accountId, subscriberId, "Leak in the bathroom"))
        }
        assertThrows<DeviceNotFoundForAccountException> {
            registerHandler.handle(RegisterIncidentCommand(UUID.randomUUID(), subscriberId, "Leak", deviceId = deviceId))
        }
    }

    @Test
    @DisplayName("US35 - Scenario 2: an incident registered from an active alert is associated with it")
    fun shouldRegisterIncidentFromActiveAlert() {
        val incident = registerHandler.handle(
            RegisterIncidentCommand(accountId, subscriberId, "Confirmed leak", alertId = activeAlertId)
        )

        assertEquals(IncidentOrigin.ALERT_DERIVED, incident.origin)
        assertEquals(activeAlertId, incident.alertId)
        assertEquals(waterPointId, incident.waterPointId)
        assertThrows<AlertNotActiveException> {
            registerHandler.handle(RegisterIncidentCommand(accountId, subscriberId, "Leak", alertId = acknowledgedAlertId))
        }
    }

    @Test
    @DisplayName("US36 - Scenario 1: requesting a visit moves a pending incident to VISIT_REQUESTED")
    fun shouldRequestTechnicalVisit() {
        val incident = requestVisit(registerOnDevice().id)

        assertEquals(IncidentState.VISIT_REQUESTED, incident.state)
        assertEquals(VisitState.REQUESTED, incident.technicalVisit!!.state)
        assertThrows<InvalidIncidentTransitionException> { requestVisit(incident.id) }
    }

    @Test
    @DisplayName("US36 - Business rule: the visit requires a plan that covers it")
    fun shouldRejectVisitWithoutCoverage() {
        coverage.covered = false
        val incident = registerOnDevice()

        assertThrows<TechnicalVisitNotCoveredException> { requestVisit(incident.id) }
    }

    @Test
    @DisplayName("US36 - Scenario 2: assigning a technician notifies the subscriber with the scheduled date")
    fun shouldAssignTechnicianAndNotify() {
        val incident = requestVisit(registerOnDevice().id)
        val scheduledAt = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0)

        val assigned = assignHandler.handle(AssignTechnicianCommand(incident.id, null, scheduledAt))

        assertEquals(IncidentState.IN_PROGRESS, assigned.state)
        assertEquals("Miguel Torres", assigned.technicalVisit!!.technicianName)
        assertEquals(scheduledAt, assigned.technicalVisit!!.scheduledAt)
        assertEquals(1, notifications.notifications.size)
    }

    @Test
    @DisplayName("US37 - Scenario 1: the list shows each incident with its date, device and current state")
    fun shouldListIncidentsWithState() {
        val first = registerOnDevice()
        val second = registerOnDevice()
        ResolveIncidentCommandHandler(incidents, noEvents).handle(ResolveIncidentCommand(second.id, "Battery replaced"))

        val listed = SupportQueryHandler(incidents, technicians, EmptyTermsRepository())
            .handle(GetIncidentsByAccountQuery(accountId))

        assertEquals(2, listed.size)
        assertEquals(setOf(IncidentState.PENDING, IncidentState.RESOLVED), listed.map { it.state }.toSet())
        listed.forEach {
            assertNotNull(it.registeredAt)
            assertEquals(deviceId, it.deviceId)
        }
        assertEquals(IncidentState.PENDING, incidents.findById(first.id)!!.state)
    }

    private class EmptyTermsRepository : TermsOfServiceRepository {
        override fun save(terms: TermsOfService): TermsOfService = terms
        override fun findLatestByLanguage(language: String): TermsOfService? = null
        override fun count(): Long = 0
    }
}
