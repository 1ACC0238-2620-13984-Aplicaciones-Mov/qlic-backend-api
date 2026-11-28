package com.wasd.qlic.monitoring.application.services

import com.wasd.qlic.monitoring.application.handlers.InMemoryIoTDeviceRepository
import com.wasd.qlic.monitoring.domain.model.DeviceDisconnectedEvent
import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.util.UUID

class DeviceConnectivityMonitorTest {

    private val repository = InMemoryIoTDeviceRepository()
    private val events = mutableListOf<Any>()
    private val monitor = DeviceConnectivityMonitor(repository, ApplicationEventPublisher { events.add(it) })

    @Test
    @DisplayName("A silent device is marked OFFLINE and notified once; a recent one is untouched")
    fun silentDeviceIsDisconnectedOnce() {
        val now = LocalDateTime.of(2026, 11, 26, 20, 0)
        val accountId = UUID.randomUUID()
        val silent = repository.save(
            IoTDevice(serialNumber = "QLIC-OFF-01", model = "QlicSense", qrCodePayload = "QR-OFF-01", accountId = accountId,
                name = "Medidor cocina", lastSyncAt = now.minusMinutes(90))
        )
        repository.save(
            IoTDevice(serialNumber = "QLIC-ON-01", model = "QlicSense", qrCodePayload = "QR-ON-01", accountId = accountId,
                lastSyncAt = now.minusMinutes(10))
        )

        assertEquals(1, monitor.checkConnectivity(now).size)
        assertEquals(DeviceStatus.OFFLINE, repository.findById(silent.id)!!.status)
        assertEquals("Medidor cocina", (events.single() as DeviceDisconnectedEvent).deviceName)

        assertEquals(0, monitor.checkConnectivity(now.plusMinutes(5)).size)
        assertEquals(1, events.size)

        repository.findById(silent.id)!!.also { it.registerReading(80, now.plusMinutes(6)) }.let { repository.save(it) }
        assertEquals(DeviceStatus.ONLINE, repository.findById(silent.id)!!.status)
    }
}
