package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.domain.exceptions.DeviceAlreadyRegisteredException
import com.wasd.qlic.monitoring.domain.exceptions.DeviceNotFoundException
import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceCommandHandlerTest {

    private lateinit var fakeRepository: InMemoryIoTDeviceRepository
    private lateinit var handler: DeviceCommandHandler

    @BeforeEach
    fun setUp() {
        fakeRepository = InMemoryIoTDeviceRepository()
        handler = DeviceCommandHandler(fakeRepository)
    }

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
        val targetWaterPointId = UUID.randomUUID()

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
}

class InMemoryIoTDeviceRepository : IoTDeviceRepository {
    private val storage = mutableMapOf<UUID, IoTDevice>()

    override fun save(device: IoTDevice): IoTDevice {
        storage[device.id] = device
        return device
    }

    override fun findById(id: UUID): IoTDevice? = storage[id]

    override fun findBySerialNumber(serialNumber: String): IoTDevice? =
        storage.values.firstOrNull { it.serialNumber == serialNumber }

    override fun findByAccountId(accountId: UUID): List<IoTDevice> =
        storage.values.filter { it.accountId == accountId }
}