package com.wasd.qlic.support.interfaces.rest

import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.interfaces.rest.resources.IncidentResource
import com.wasd.qlic.support.interfaces.rest.resources.TechnicalVisitResource

/** Transforms the Incident aggregate into its REST representation. */
fun Incident.toResource() = IncidentResource(
    id = id,
    accountId = accountId,
    description = description,
    deviceId = deviceId,
    waterPointId = waterPointId,
    alertId = alertId,
    origin = origin.name,
    state = state.name,
    registeredAt = registeredAt,
    updatedAt = updatedAt,
    technicalVisit = technicalVisit?.let {
        TechnicalVisitResource(
            id = it.id,
            preferredDate = it.availability.preferredDate,
            fromTime = it.availability.fromTime,
            toTime = it.availability.toTime,
            notes = it.notes,
            state = it.state.name,
            technicianId = it.technicianId,
            technicianName = it.technicianName,
            scheduledAt = it.scheduledAt,
            requestedAt = it.requestedAt
        )
    },
    resolution = resolution?.description,
    resolvedAt = resolution?.resolvedAt
)
