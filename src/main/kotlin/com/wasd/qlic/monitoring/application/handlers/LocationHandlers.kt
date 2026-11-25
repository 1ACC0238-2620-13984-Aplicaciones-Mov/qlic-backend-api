package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.CreateLocationCommand
import com.wasd.qlic.monitoring.application.commands.CreateWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.DeleteLocationCommand
import com.wasd.qlic.monitoring.application.commands.DeleteWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.UpdateLocationCommand
import com.wasd.qlic.monitoring.application.commands.UpdateWaterPointCommand
import com.wasd.qlic.monitoring.application.queries.GetLocationByIdQuery
import com.wasd.qlic.monitoring.application.queries.GetLocationsQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterPointByIdQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterPointsQuery
import com.wasd.qlic.monitoring.domain.exceptions.InvalidLocationDataException
import com.wasd.qlic.monitoring.domain.exceptions.LocationHasWaterPointsException
import com.wasd.qlic.monitoring.domain.exceptions.LocationNotFoundException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointHasDevicesException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointLimitReachedException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointNotFoundException
import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import com.wasd.qlic.monitoring.domain.services.PlanEntitlementProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/** US13: administration of the locations (premises or homes) of the account. */
@Component
class LocationCommandHandler(
    private val locationRepository: LocationRepository,
    private val waterPointRepository: WaterPointRepository
) {
    @Transactional
    fun handle(command: CreateLocationCommand): Location =
        locationRepository.save(validated { Location(accountId = command.accountId, name = command.name, address = command.address) })

    @Transactional
    fun handle(command: UpdateLocationCommand): Location {
        val location = ownedLocation(locationRepository, command.locationId, command.accountId)
        validated { location.rename(command.name, command.address) }
        return locationRepository.save(location)
    }

    @Transactional
    fun handle(command: DeleteLocationCommand) {
        val location = ownedLocation(locationRepository, command.locationId, command.accountId)
        if (waterPointRepository.existsByLocationId(location.id)) throw LocationHasWaterPointsException()
        locationRepository.delete(location)
    }

    @Transactional(readOnly = true)
    fun handle(query: GetLocationsQuery): List<Location> = locationRepository.findByAccountId(query.accountId)

    @Transactional(readOnly = true)
    fun handle(query: GetLocationByIdQuery): Location = ownedLocation(locationRepository, query.locationId, query.accountId)
}

/** US13: administration of the Water Points of the account, limited by the plan in force. */
@Component
class WaterPointCommandHandler(
    private val waterPointRepository: WaterPointRepository,
    private val locationRepository: LocationRepository,
    private val deviceRepository: IoTDeviceRepository,
    private val planEntitlementProvider: PlanEntitlementProvider
) {
    @Transactional
    fun handle(command: CreateWaterPointCommand): WaterPoint {
        ownedLocation(locationRepository, command.locationId, command.accountId)
        val limit = planEntitlementProvider.maxWaterPoints(command.accountId)
        if (waterPointRepository.countByAccountId(command.accountId) >= limit) throw WaterPointLimitReachedException(limit)
        return waterPointRepository.save(
            validated {
                WaterPoint(accountId = command.accountId, locationId = command.locationId, name = command.name, type = command.type)
            }
        )
    }

    @Transactional
    fun handle(command: UpdateWaterPointCommand): WaterPoint {
        val waterPoint = ownedWaterPoint(command.waterPointId, command.accountId)
        ownedLocation(locationRepository, command.locationId, command.accountId)
        validated { waterPoint.update(command.name, command.type, command.locationId) }
        return waterPointRepository.save(waterPoint)
    }

    @Transactional
    fun handle(command: DeleteWaterPointCommand) {
        val waterPoint = ownedWaterPoint(command.waterPointId, command.accountId)
        if (deviceRepository.existsByWaterPointId(waterPoint.id)) throw WaterPointHasDevicesException()
        waterPointRepository.delete(waterPoint)
    }

    @Transactional(readOnly = true)
    fun handle(query: GetWaterPointsQuery): List<WaterPoint> {
        val locationId = query.locationId ?: return waterPointRepository.findByAccountId(query.accountId)
        ownedLocation(locationRepository, locationId, query.accountId)
        return waterPointRepository.findByLocationId(locationId)
    }

    @Transactional(readOnly = true)
    fun handle(query: GetWaterPointByIdQuery): WaterPoint = ownedWaterPoint(query.waterPointId, query.accountId)

    private fun ownedWaterPoint(waterPointId: UUID, accountId: UUID): WaterPoint =
        waterPointRepository.findById(waterPointId)?.takeIf { it.accountId == accountId } ?: throw WaterPointNotFoundException()
}

internal fun ownedLocation(repository: LocationRepository, locationId: UUID, accountId: UUID): Location =
    repository.findById(locationId)?.takeIf { it.accountId == accountId } ?: throw LocationNotFoundException()

/** Translates the invariants of the domain (require) into a localized 400 response. */
private inline fun <T> validated(block: () -> T): T = try {
    block()
} catch (exception: IllegalArgumentException) {
    throw InvalidLocationDataException()
}
