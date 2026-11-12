package com.wasd.qlic.support.application.handlers

import com.wasd.qlic.support.application.commands.RegisterIncidentCommand
import com.wasd.qlic.support.domain.exceptions.AlertNotActiveException
import com.wasd.qlic.support.domain.exceptions.AlertNotFoundForAccountException
import com.wasd.qlic.support.domain.exceptions.DeviceNotFoundForAccountException
import com.wasd.qlic.support.domain.exceptions.IncidentAssetRequiredException
import com.wasd.qlic.support.domain.model.Incident
import com.wasd.qlic.support.domain.model.IncidentRegisteredEvent
import com.wasd.qlic.support.domain.repositories.IncidentRepository
import com.wasd.qlic.support.domain.services.AlertDataProvider
import com.wasd.qlic.support.domain.services.DeviceDataProvider
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US35: retrieves the alert data when applicable, creates the incident in PENDING state
 * and publishes IncidentRegisteredEvent.
 */
@Component
class RegisterIncidentCommandHandler(
    private val incidentRepository: IncidentRepository,
    private val alertDataProvider: AlertDataProvider,
    private val deviceDataProvider: DeviceDataProvider,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: RegisterIncidentCommand): Incident {
        if (command.deviceId == null && command.waterPointId == null && command.alertId == null) {
            throw IncidentAssetRequiredException()
        }

        val alert = command.alertId?.let {
            val data = alertDataProvider.findAlert(it, command.accountId) ?: throw AlertNotFoundForAccountException()
            if (!data.active) throw AlertNotActiveException()
            data
        }
        val device = command.deviceId?.let {
            deviceDataProvider.findDevice(it, command.accountId) ?: throw DeviceNotFoundForAccountException()
        }

        val incident = incidentRepository.save(
            Incident(
                accountId = command.accountId,
                reportedBy = command.reportedBy,
                description = command.description.trim(),
                deviceId = device?.deviceId,
                waterPointId = alert?.waterPointId ?: command.waterPointId ?: device?.waterPointId,
                alertId = alert?.alertId
            )
        )
        eventPublisher.publishEvent(
            IncidentRegisteredEvent(
                incidentId = incident.id,
                accountId = incident.accountId,
                origin = incident.origin.name,
                alertId = incident.alertId
            )
        )
        return incident
    }
}
