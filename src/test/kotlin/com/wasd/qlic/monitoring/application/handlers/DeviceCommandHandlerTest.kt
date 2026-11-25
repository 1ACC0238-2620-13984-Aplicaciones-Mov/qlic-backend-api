package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.domain.exceptions.DeviceAlreadyRegisteredException
import com.wasd.qlic.monitoring.domain.exceptions.DeviceNotFoundException
import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointNotFoundException
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceCommandHandlerTest {

    private lateinit var fakeRepository: InMemoryIoTDeviceRepository
    private lateinit var waterPoints: InMemoryWaterPointRepository
    private lateinit var locations: InMemoryLocationRepository
    private lateinit var handler: DeviceCommandHandler

    @BeforeEach
    fun setUp() {
        fakeRepository = InMemoryIoTDeviceRepository()
        waterPoints = InMemoryWaterPointRepository()
        locations = InMemoryLocationRepository()
        handler = DeviceCommandHandler(fakeRepository, waterPoints, locations)
    }

    private fun waterPointOf(accountId: UUID, locationId: UUID = UUID.randomUUID()): WaterPoint =
        waterPoints.save(WaterPoint(accountId = accountId, locationId = locationId, name = "Cocina"))

    @Test
    @DisplayName("US10: Registrar dispositivo IoT nuevo persiste con estado ONLINE")
    fun shouldRegisterNewDeviceSuccessfully() {
        val accountId = UUID.randomUUID()
        val command = RegisterDeviceCommand(
            qrCodePayload = "QR-TEST-001",
            serialNumber = "SN-001",
            model = "FlowMeter",
            accountId = accountId
        )

        val result = handler.handleRegister(command)

        assertNotNull(result.id)
        assertEquals("SN-001", result.serialNumber)
        assertEquals(DeviceStatus.ONLINE, result.status)
        assertEquals(100, result.batteryPercentage)
    }

    @Test
    @DisplayName("US11: Asignar Water Point actualiza la vinculacion en el dispositivo")
    fun shouldAssignWaterPointToExistingDevice() {
        val accountId = UUID.randomUUID()
        val device = fakeRepository.save(
            IoTDevice(
                serialNumber = "SN-002",
                model = "FlowMeter",
                qrCodePayload = "QR-TEST-002",
                accountId = accountId
            )
        )
        val targetWaterPointId = waterPointOf(accountId).id

        val updated = handler.handleAssignWaterPoint(
            AssignWaterPointCommand(deviceId = device.id, waterPointId = targetWaterPointId, accountId = accountId)
        )

        assertEquals(targetWaterPointId, updated.waterPointId)
    }

    @Test
    @DisplayName("US11: a device of another account cannot be assigned to a Water Point")
    fun shouldRejectAssigningDeviceOfAnotherAccount() {
        val device = fakeRepository.save(
            IoTDevice(serialNumber = "SN-003", model = "FlowMeter", qrCodePayload = "QR-TEST-003", accountId = UUID.randomUUID())
        )

        assertThrows(DeviceNotFoundException::class.java) {
            handler.handleAssignWaterPoint(
                AssignWaterPointCommand(deviceId = device.id, waterPointId = UUID.randomUUID(), accountId = UUID.randomUUID())
            )
        }
    }

    @Test
    @DisplayName("US10: a serial number registered in another account is rejected")
    fun shouldRejectSerialNumberOfAnotherAccount() {
        val owner = UUID.randomUUID()
        handler.handleRegister(RegisterDeviceCommand("QR-TEST-004", "SN-004", "FlowMeter", owner))

        assertThrows(DeviceAlreadyRegisteredException::class.java) {
            handler.handleRegister(RegisterDeviceCommand("QR-TEST-004", "SN-004", "FlowMeter", UUID.randomUUID()))
        }
        assertEquals(owner, handler.handleRegister(RegisterDeviceCommand("QR-TEST-004", "SN-004", "FlowMeter", owner)).accountId)
    }

    @Test
    @DisplayName("US11: a Water Point of another account cannot be assigned")
    fun shouldRejectWaterPointOfAnotherAccount() {
        val accountId = UUID.randomUUID()
        val device = fakeRepository.save(
            IoTDevice(serialNumber = "SN-005", model = "FlowMeter", qrCodePayload = "QR-TEST-005", accountId = accountId)
        )
        val foreignWaterPoint = waterPointOf(UUID.randomUUID())

        assertThrows(WaterPointNotFoundException::class.java) {
            handler.handleAssignWaterPoint(AssignWaterPointCommand(device.id, foreignWaterPoint.id, accountId))
        }
    }

    @Test
    @DisplayName("US13 - Scenario 1: only the devices of the requested location are returned")
    fun shouldFilterDevicesByLocation() {
        val accountId = UUID.randomUUID()
        val miraflores = locations.save(Location(accountId = accountId, name = "Miraflores"))
        val surco = locations.save(Location(accountId = accountId, name = "Surco"))
        val kitchen = waterPointOf(accountId, miraflores.id)
        val garden = waterPointOf(accountId, surco.id)
        listOf("SN-006" to kitchen.id, "SN-007" to garden.id).forEach { (serial, waterPointId) ->
            fakeRepository.save(
                IoTDevice(serialNumber = serial, model = "FlowMeter", qrCodePayload = "QR-$serial", accountId = accountId)
                    .also { it.assignWaterPoint(waterPointId) }
            )
        }

        val result = handler.handleGetByAccount(GetDevicesByAccountQuery(accountId, miraflores.id))

        assertEquals(listOf("SN-006"), result.map { it.serialNumber })
        assertEquals(2, handler.handleGetByAccount(GetDevicesByAccountQuery(accountId)).size)
    }
}
