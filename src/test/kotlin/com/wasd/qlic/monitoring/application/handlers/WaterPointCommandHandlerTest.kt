package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.CreateLocationCommand
import com.wasd.qlic.monitoring.application.commands.CreateWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.DeleteLocationCommand
import com.wasd.qlic.monitoring.application.commands.DeleteWaterPointCommand
import com.wasd.qlic.monitoring.domain.exceptions.InvalidLocationDataException
import com.wasd.qlic.monitoring.domain.exceptions.LocationHasWaterPointsException
import com.wasd.qlic.monitoring.domain.exceptions.LocationNotFoundException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointHasDevicesException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointLimitReachedException
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.WaterPointType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class WaterPointCommandHandlerTest {

    private val accountId = UUID.randomUUID()
    private lateinit var devices: InMemoryIoTDeviceRepository
    private lateinit var waterPoints: InMemoryWaterPointRepository
    private lateinit var locations: InMemoryLocationRepository
    private lateinit var plan: FixedPlanEntitlementProvider
    private lateinit var locationHandler: LocationCommandHandler
    private lateinit var waterPointHandler: WaterPointCommandHandler

    @BeforeEach
    fun setUp() {
        devices = InMemoryIoTDeviceRepository()
        waterPoints = InMemoryWaterPointRepository()
        locations = InMemoryLocationRepository()
        plan = FixedPlanEntitlementProvider(maxWaterPoints = 2)
        locationHandler = LocationCommandHandler(locations, waterPoints, InMemoryWaterTankRepository())
        waterPointHandler = WaterPointCommandHandler(waterPoints, locations, devices, plan)
    }

    private fun location(name: String = "Restaurante Miraflores") =
        locationHandler.handle(CreateLocationCommand(accountId, name, "Av. Larco 345"))

    private fun waterPoint(locationId: UUID, name: String = "Cocina") =
        waterPointHandler.handle(CreateWaterPointCommand(accountId, locationId, name, WaterPointType.KITCHEN))

    @Test
    @DisplayName("US13: a Water Point is registered in a location of the account")
    fun shouldRegisterWaterPoint() {
        val location = location()

        val created = waterPoint(location.id, "  Cocina principal ")

        assertEquals("Cocina principal", created.name)
        assertEquals(location.id, created.locationId)
        assertEquals(WaterPointType.KITCHEN, created.type)
    }

    @Test
    @DisplayName("Business rule: the plan limits the number of Water Points")
    fun shouldRejectWaterPointBeyondPlanLimit() {
        val location = location()
        waterPoint(location.id, "Cocina")
        waterPoint(location.id, "Baño")

        assertThrows(WaterPointLimitReachedException::class.java) { waterPoint(location.id, "Jardín") }

        plan.maxWaterPoints = 3
        assertEquals("Jardín", waterPoint(location.id, "Jardín").name)
    }

    @Test
    @DisplayName("US13: a Water Point cannot be registered in a location of another account")
    fun shouldRejectLocationOfAnotherAccount() {
        val foreign = locationHandler.handle(CreateLocationCommand(UUID.randomUUID(), "Otro local", null))

        assertThrows(LocationNotFoundException::class.java) { waterPoint(foreign.id) }
    }

    @Test
    @DisplayName("A location with Water Points and a Water Point with devices cannot be deleted")
    fun shouldProtectDeletionWhileInUse() {
        val location = location()
        val kitchen = waterPoint(location.id)
        devices.save(
            IoTDevice(serialNumber = "SN-100", model = "QlicSense", qrCodePayload = "QR-100", accountId = accountId)
                .also { it.assignWaterPoint(kitchen.id) }
        )

        assertThrows(LocationHasWaterPointsException::class.java) {
            locationHandler.handle(DeleteLocationCommand(location.id, accountId))
        }
        assertThrows(WaterPointHasDevicesException::class.java) {
            waterPointHandler.handle(DeleteWaterPointCommand(kitchen.id, accountId))
        }
    }

    @Test
    @DisplayName("A blank name is rejected with a localized validation error")
    fun shouldRejectBlankName() {
        assertThrows(InvalidLocationDataException::class.java) { location("   ") }
    }
}
