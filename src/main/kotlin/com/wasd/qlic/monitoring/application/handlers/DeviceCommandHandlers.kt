package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import com.wasd.qlic.monitoring.domain.exceptions.DeviceAlreadyRegisteredException
import com.wasd.qlic.monitoring.domain.exceptions.DeviceNotFoundException
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.springframework.stereotype.Component

@Component
class DeviceCommandHandler(
    private val deviceRepository: IoTDeviceRepository
) {
    /**
     * US10: registers the device in the account of the session. Registering the same serial number
     * again in the same account is idempotent; a serial number of another account is rejected.
     */
    fun handleRegister(command: RegisterDeviceCommand): IoTDevice {
        val existing = deviceRepository.findBySerialNumber(command.serialNumber)
        if (existing != null) {
            if (existing.accountId != command.accountId) {
                throw DeviceAlreadyRegisteredException(command.serialNumber)
            }
            return existing
        }

        val device = IoTDevice(
            serialNumber = command.serialNumber,
            model = command.model,
            qrCodePayload = command.qrCodePayload,
            accountId = command.accountId
        )
        return deviceRepository.save(device)
    }

    /** US11: assigns a device of the account of the session to a Water Point. */
    fun handleAssignWaterPoint(command: AssignWaterPointCommand): IoTDevice {
        val device = deviceRepository.findById(command.deviceId)
            ?.takeIf { it.accountId == command.accountId }
            ?: throw DeviceNotFoundException()
        device.assignWaterPoint(command.waterPointId)
        return deviceRepository.save(device)
    }

    /** US12: lists the devices of the account of the session. */
    fun handleGetByAccount(query: GetDevicesByAccountQuery): List<IoTDevice> =
        deviceRepository.findByAccountId(query.accountId)
}
