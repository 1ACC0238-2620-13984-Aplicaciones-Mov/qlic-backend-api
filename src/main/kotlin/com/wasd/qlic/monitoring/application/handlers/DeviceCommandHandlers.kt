package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.springframework.stereotype.Component

@Component
class DeviceCommandHandler(
    private val deviceRepository: IoTDeviceRepository
) {
    // Handler US10[cite: 5]
    fun handleRegister(command: RegisterDeviceCommand): IoTDevice {
        val existing = deviceRepository.findBySerialNumber(command.serialNumber)
        if (existing != null) {
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

    // Handler US11[cite: 5]
    fun handleAssignWaterPoint(command: AssignWaterPointCommand): IoTDevice? {
        val device = deviceRepository.findById(command.deviceId) ?: return null
        device.assignWaterPoint(command.waterPointId)
        return deviceRepository.save(device)
    }

    // Handler US12[cite: 5]
    fun handleGetByAccount(query: GetDevicesByAccountQuery): List<IoTDevice> {
        return deviceRepository.findByAccountId(query.accountId)
    }
}