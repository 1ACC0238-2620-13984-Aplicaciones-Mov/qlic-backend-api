package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.DeleteDeviceCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.application.commands.UpdateDeviceCommand
import com.wasd.qlic.monitoring.application.queries.GetDeviceByIdQuery
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import com.wasd.qlic.monitoring.domain.exceptions.DeviceAlreadyRegisteredException
import com.wasd.qlic.monitoring.domain.exceptions.DeviceNotFoundException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointNotFoundException
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DeviceCommandHandler(
    private val deviceRepository: IoTDeviceRepository,
    private val waterPointRepository: WaterPointRepository,
    private val locationRepository: LocationRepository
) {
    /**
     * US10: registers the device in the account of the session. Registering the same serial number
     * again in the same account is idempotent; a serial number of another account is rejected.
     */
    @Transactional
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

    /** US11: assigns a device of the account to one of its Water Points. */
    @Transactional
    fun handleAssignWaterPoint(command: AssignWaterPointCommand): IoTDevice {
        val device = ownedDevice(command.deviceId, command.accountId)
        waterPointRepository.findById(command.waterPointId)?.takeIf { it.accountId == command.accountId }
            ?: throw WaterPointNotFoundException()
        device.assignWaterPoint(command.waterPointId)
        return deviceRepository.save(device)
    }

    /**
     * TS02: renames the device and assigns it to a Water Point of the account, or unassigns it when
     * waterPointId is null. The readings already registered keep their original Water Point.
     */
    @Transactional
    fun handleUpdate(command: UpdateDeviceCommand): IoTDevice {
        val device = ownedDevice(command.deviceId, command.accountId)
        device.rename(command.name)
        val waterPointId = command.waterPointId
        if (waterPointId == null) {
            device.unassignWaterPoint()
        } else {
            waterPointRepository.findById(waterPointId)?.takeIf { it.accountId == command.accountId }
                ?: throw WaterPointNotFoundException()
            device.assignWaterPoint(waterPointId)
        }
        return deviceRepository.save(device)
    }

    /** Unlinks a device from the account; its readings are kept for the reports. */
    @Transactional
    fun handleDelete(command: DeleteDeviceCommand) {
        deviceRepository.delete(ownedDevice(command.deviceId, command.accountId))
    }

    /** US12: devices of the account. US13: only those assigned to the Water Points of one location. */
    @Transactional(readOnly = true)
    fun handleGetByAccount(query: GetDevicesByAccountQuery): List<IoTDevice> {
        val devices = deviceRepository.findByAccountId(query.accountId)
        val locationId = query.locationId ?: return devices
        ownedLocation(locationRepository, locationId, query.accountId)
        val waterPointIds = waterPointRepository.findByLocationId(locationId).map { it.id }.toSet()
        return devices.filter { it.waterPointId in waterPointIds }
    }

    @Transactional(readOnly = true)
    fun handleGetById(query: GetDeviceByIdQuery): IoTDevice = ownedDevice(query.deviceId, query.accountId)

    private fun ownedDevice(deviceId: UUID, accountId: UUID): IoTDevice =
        deviceRepository.findById(deviceId)?.takeIf { it.accountId == accountId } ?: throw DeviceNotFoundException()
}
