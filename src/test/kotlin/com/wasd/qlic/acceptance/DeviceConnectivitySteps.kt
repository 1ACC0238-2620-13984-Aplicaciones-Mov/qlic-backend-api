package com.wasd.qlic.acceptance

import com.wasd.qlic.monitoring.application.handlers.InMemoryIoTDeviceRepository
import com.wasd.qlic.monitoring.application.services.DeviceConnectivityMonitor
import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import java.util.UUID

/** Steps of medidor_desconectado.feature, on the connectivity check that runs every few minutes. */
class DeviceConnectivitySteps {

    private val repository = InMemoryIoTDeviceRepository()
    private val notifications = mutableListOf<Any>()
    private val monitor = DeviceConnectivityMonitor(repository, ApplicationEventPublisher { notifications.add(it) })
    private val now = LocalDateTime.of(2026, 11, 26, 20, 0)
    private lateinit var device: IoTDevice

    @Given("un medidor que reportó por última vez hace {int} minutos")
    fun deviceLastReported(minutes: Int) {
        device = repository.save(
            IoTDevice(
                serialNumber = "QLIC-BDD-${UUID.randomUUID()}", model = "QlicSense", qrCodePayload = "QR-BDD",
                accountId = UUID.randomUUID(), name = "Medidor baño", lastSyncAt = now.minusMinutes(minutes.toLong())
            )
        )
    }

    @When("se revisa la conexión de los medidores")
    fun checkConnectivity() {
        monitor.checkConnectivity(now)
    }

    @Then("el medidor queda en estado {string}")
    fun deviceStatus(status: String) {
        assertEquals(DeviceStatus.valueOf(status), repository.findById(device.id)!!.status)
    }

    @Then("se avisó al titular {int} vez")
    fun notifiedOnce(times: Int) {
        assertEquals(times, notifications.size)
    }

    @Then("se avisó al titular {int} veces")
    fun notifiedTimes(times: Int) {
        assertEquals(times, notifications.size)
    }
}
