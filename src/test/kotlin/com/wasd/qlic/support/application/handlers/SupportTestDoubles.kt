package com.wasd.qlic.support.application.handlers

import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.domain.model.IncidentState
import com.wasd.qlic.support.domain.model.Technician
import com.wasd.qlic.support.domain.repositories.IncidentRepository
import com.wasd.qlic.support.domain.repositories.TechnicianRepository
import com.wasd.qlic.support.domain.services.AlertData
import com.wasd.qlic.support.domain.services.AlertDataProvider
import com.wasd.qlic.support.domain.services.DeviceData
import com.wasd.qlic.support.domain.services.DeviceDataProvider
import com.wasd.qlic.support.domain.services.PlanEntitlementProvider
import com.wasd.qlic.support.domain.services.SupportNotificationSender
import java.time.LocalDateTime
import java.util.UUID

class InMemoryIncidentRepository : IncidentRepository {
    private val storage = mutableMapOf<UUID, Incident>()
    override fun save(incident: Incident): Incident = incident.also { storage[it.id] = it }
    override fun findById(id: UUID): Incident? = storage[id]
    override fun findByAccountId(accountId: UUID): List<Incident> = storage.values.filter { it.accountId == accountId }
    override fun findByState(state: IncidentState): List<Incident> = storage.values.filter { it.state == state }
}

class InMemoryTechnicianRepository : TechnicianRepository {
    private val storage = linkedMapOf<UUID, Technician>()
    override fun save(technician: Technician): Technician = technician.also { storage[it.id] = it }
    override fun findById(id: UUID): Technician? = storage[id]
    override fun findAllActive(): List<Technician> = storage.values.filter { it.active }
    override fun count(): Long = storage.size.toLong()
}

class FakeAlertDataProvider(private val alerts: Map<UUID, Pair<UUID, AlertData>>) : AlertDataProvider {
    override fun findAlert(alertId: UUID, accountId: UUID): AlertData? =
        alerts[alertId]?.takeIf { it.first == accountId }?.second
}

class FakeDeviceDataProvider(private val devices: Map<UUID, Pair<UUID, DeviceData>>) : DeviceDataProvider {
    override fun findDevice(deviceId: UUID, accountId: UUID): DeviceData? =
        devices[deviceId]?.takeIf { it.first == accountId }?.second
}

class FixedCoverageProvider(var covered: Boolean) : PlanEntitlementProvider {
    override fun coversTechnicalVisits(accountId: UUID): Boolean = covered
}

class RecordingSupportNotificationSender : SupportNotificationSender {
    val notifications = mutableListOf<String>()
    override fun notifyTechnicianAssigned(accountId: UUID, incidentId: UUID, technicianName: String, scheduledAt: LocalDateTime) {
        notifications += "$incidentId:$technicianName:$scheduledAt"
    }
}
