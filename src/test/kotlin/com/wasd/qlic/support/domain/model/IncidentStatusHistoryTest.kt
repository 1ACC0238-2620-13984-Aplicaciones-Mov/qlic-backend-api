package com.wasd.qlic.support.domain.model

import com.wasd.qlic.support.domain.exceptions.InvalidIncidentTransitionException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** US37: the incident keeps every state it went through. */
class IncidentStatusHistoryTest {

    private fun newIncident() = Incident(
        accountId = UUID.randomUUID(),
        reportedBy = UUID.randomUUID(),
        description = "Fuga en la tubería de la cocina",
        deviceId = UUID.randomUUID()
    )

    @Test
    @DisplayName("US37: a new incident starts its history with the registration in PENDING")
    fun historyStartsWithRegistration() {
        val incident = newIncident()

        assertEquals(1, incident.history.size)
        assertNull(incident.history.first().fromState)
        assertEquals(IncidentState.PENDING, incident.history.first().toState)
        assertEquals(incident.registeredAt, incident.history.first().occurredAt)
    }

    @Test
    @DisplayName("US37: visit, technician and resolution are recorded in order with their detail")
    fun historyRecordsTheAttentionCycle() {
        val incident = newIncident()
        val preferredDate = LocalDate.now().plusDays(2)
        val scheduledAt = LocalDateTime.of(preferredDate, LocalTime.of(10, 0))

        incident.requestTechnicalVisit(AvailabilityWindow(preferredDate, LocalTime.of(9, 0), LocalTime.of(13, 0)), null)
        incident.assignTechnician(Technician(fullName = "Miguel Torres", phone = "+51987000111", zone = "Lima Centro"), scheduledAt)
        incident.resolve(Resolution("Se reemplazó la llave de paso"))

        assertEquals(
            listOf(IncidentState.PENDING, IncidentState.VISIT_REQUESTED, IncidentState.IN_PROGRESS, IncidentState.RESOLVED),
            incident.history.map { it.toState }
        )
        assertEquals(
            listOf(null, IncidentState.PENDING, IncidentState.VISIT_REQUESTED, IncidentState.IN_PROGRESS),
            incident.history.map { it.fromState }
        )
        assertEquals(preferredDate.toString(), incident.history[1].detail)
        assertEquals("Miguel Torres - $scheduledAt", incident.history[2].detail)
        assertEquals("Se reemplazó la llave de paso", incident.history[3].detail)
        assertEquals(incident.updatedAt, incident.history.last().occurredAt)
    }

    @Test
    @DisplayName("US37: an invalid transition is rejected and leaves the history unchanged")
    fun invalidTransitionDoesNotChangeHistory() {
        val incident = newIncident()
        incident.resolve(Resolution("Falsa alarma"))

        assertThrows(InvalidIncidentTransitionException::class.java) {
            incident.requestTechnicalVisit(AvailabilityWindow(LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(12, 0)), null)
        }
        assertEquals(2, incident.history.size)
    }
}
