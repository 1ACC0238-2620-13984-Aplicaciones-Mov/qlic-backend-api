package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import com.wasd.qlic.monitoring.domain.services.PlanEntitlementProvider
import java.time.LocalDateTime
import java.util.UUID

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

    override fun existsByWaterPointId(waterPointId: UUID): Boolean =
        storage.values.any { it.waterPointId == waterPointId }

    override fun delete(device: IoTDevice) {
        storage.remove(device.id)
    }
}

class InMemoryLocationRepository : LocationRepository {
    private val storage = mutableMapOf<UUID, Location>()

    override fun save(location: Location): Location {
        storage[location.id] = location
        return location
    }

    override fun findById(id: UUID): Location? = storage[id]

    override fun findByAccountId(accountId: UUID): List<Location> =
        storage.values.filter { it.accountId == accountId }.sortedBy { it.name }

    override fun delete(location: Location) {
        storage.remove(location.id)
    }
}

class InMemoryWaterPointRepository : WaterPointRepository {
    private val storage = mutableMapOf<UUID, WaterPoint>()

    override fun save(waterPoint: WaterPoint): WaterPoint {
        storage[waterPoint.id] = waterPoint
        return waterPoint
    }

    override fun findById(id: UUID): WaterPoint? = storage[id]

    override fun findByAccountId(accountId: UUID): List<WaterPoint> =
        storage.values.filter { it.accountId == accountId }.sortedBy { it.name }

    override fun findByLocationId(locationId: UUID): List<WaterPoint> =
        storage.values.filter { it.locationId == locationId }.sortedBy { it.name }

    override fun countByAccountId(accountId: UUID): Int = storage.values.count { it.accountId == accountId }

    override fun existsByLocationId(locationId: UUID): Boolean = storage.values.any { it.locationId == locationId }

    override fun delete(waterPoint: WaterPoint) {
        storage.remove(waterPoint.id)
    }
}

class FixedPlanEntitlementProvider(var maxWaterPoints: Int = 2) : PlanEntitlementProvider {
    override fun maxWaterPoints(accountId: UUID): Int = maxWaterPoints
}

class InMemoryConsumptionReadingRepository : ConsumptionReadingRepository {
    private val storage = mutableListOf<ConsumptionReading>()

    override fun save(reading: ConsumptionReading): ConsumptionReading {
        storage.add(reading)
        return reading
    }

    override fun findByAccountIdBetween(accountId: UUID, from: LocalDateTime, to: LocalDateTime): List<ConsumptionReading> =
        storage.filter { it.accountId == accountId && !it.recordedAt.isBefore(from) && !it.recordedAt.isAfter(to) }
}
