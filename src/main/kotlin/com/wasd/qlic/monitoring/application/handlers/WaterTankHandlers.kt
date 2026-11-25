package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.DeleteWaterTankCommand
import com.wasd.qlic.monitoring.application.commands.RegisterTankLevelCommand
import com.wasd.qlic.monitoring.application.commands.SaveWaterTankCommand
import com.wasd.qlic.monitoring.application.queries.GetTankLevelsQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterTankQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterTanksQuery
import com.wasd.qlic.monitoring.domain.exceptions.DeviceNotFoundException
import com.wasd.qlic.monitoring.domain.exceptions.InvalidReadingPeriodException
import com.wasd.qlic.monitoring.domain.exceptions.InvalidWaterTankException
import com.wasd.qlic.monitoring.domain.exceptions.WaterTankNotFoundException
import com.wasd.qlic.monitoring.domain.model.ReplenishmentPrediction
import com.wasd.qlic.monitoring.domain.model.TankLevelLowEvent
import com.wasd.qlic.monitoring.domain.model.TankLevelReading
import com.wasd.qlic.monitoring.domain.model.WaterTank
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.TankLevelReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterTankRepository
import com.wasd.qlic.monitoring.domain.services.ReplenishmentPredictionService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/** A tank with the prediction of when it will reach its critical level. */
data class WaterTankDetail(val tank: WaterTank, val prediction: ReplenishmentPrediction)

/** Result of registering a level: the updated tank and whether the low level alert was raised (US29). */
data class TankLevelRegistration(val tank: WaterTank, val reading: TankLevelReading, val lowLevelAlertRaised: Boolean)

/** US28, US29, US30: administration of the tanks, ingestion of their levels and replenishment prediction. */
@Component
class WaterTankCommandHandler(
    private val tankRepository: WaterTankRepository,
    private val levelRepository: TankLevelReadingRepository,
    private val locationRepository: LocationRepository,
    private val deviceRepository: IoTDeviceRepository,
    private val predictionService: ReplenishmentPredictionService,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: SaveWaterTankCommand): WaterTank {
        ownedLocation(locationRepository, command.locationId, command.accountId)
        command.deviceId?.let { deviceId ->
            deviceRepository.findById(deviceId)?.takeIf { it.accountId == command.accountId } ?: throw DeviceNotFoundException()
        }
        val tank = tankOrInvalid {
            command.tankId?.let { id ->
                ownedTank(id, command.accountId).also {
                    it.update(command.name, command.locationId, command.capacityLiters, command.criticalLevelPercentage, command.deviceId)
                }
            } ?: WaterTank(
                accountId = command.accountId,
                locationId = command.locationId,
                name = command.name,
                capacityLiters = command.capacityLiters,
                criticalLevelPercentage = command.criticalLevelPercentage,
                deviceId = command.deviceId
            )
        }
        return tankRepository.save(tank)
    }

    @Transactional
    fun handle(command: DeleteWaterTankCommand) {
        val tank = ownedTank(command.tankId, command.accountId)
        levelRepository.deleteByTankId(tank.id)
        tankRepository.delete(tank)
    }

    /** US29 - Scenario 1: crossing the critical level publishes TankLevelLowEvent with the tank and its level. */
    @Transactional
    fun handle(command: RegisterTankLevelCommand): TankLevelRegistration {
        val tank = ownedTank(command.tankId, command.accountId)
        val recordedAt = command.recordedAt ?: LocalDateTime.now()
        val raised = tankOrInvalid { tank.registerLevel(command.levelPercentage, recordedAt) }
        val reading = levelRepository.save(
            TankLevelReading(tankId = tank.id, accountId = tank.accountId, levelPercentage = command.levelPercentage, recordedAt = recordedAt)
        )
        val saved = tankRepository.save(tank)
        if (raised) {
            eventPublisher.publishEvent(
                TankLevelLowEvent(
                    accountId = saved.accountId,
                    tankId = saved.id,
                    tankName = saved.name,
                    levelPercentage = command.levelPercentage,
                    criticalLevelPercentage = saved.criticalLevelPercentage,
                    occurredAt = recordedAt
                )
            )
        }
        return TankLevelRegistration(saved, reading, raised)
    }

    @Transactional(readOnly = true)
    fun handle(query: GetWaterTanksQuery): List<WaterTank> {
        val tanks = tankRepository.findByAccountId(query.accountId)
        val locationId = query.locationId ?: return tanks
        ownedLocation(locationRepository, locationId, query.accountId)
        return tanks.filter { it.locationId == locationId }
    }

    /** US28 - Scenario 1 and US30: current level, last reading and prediction from the last 7 days. */
    @Transactional(readOnly = true)
    fun handle(query: GetWaterTankQuery): WaterTankDetail {
        val tank = ownedTank(query.tankId, query.accountId)
        val to = tank.lastReadingAt ?: return WaterTankDetail(tank, ReplenishmentPrediction.UNAVAILABLE)
        val history = levelRepository.findByTankIdBetween(tank.id, to.minusDays(PREDICTION_HISTORY_DAYS), to)
        return WaterTankDetail(tank, predictionService.predict(tank, history))
    }

    @Transactional(readOnly = true)
    fun handle(query: GetTankLevelsQuery): List<TankLevelReading> {
        if (query.to.isBefore(query.from) || ChronoUnit.DAYS.between(query.from, query.to) > MAX_HISTORY_DAYS) {
            throw InvalidReadingPeriodException()
        }
        val tank = ownedTank(query.tankId, query.accountId)
        return levelRepository.findByTankIdBetween(tank.id, query.from.atStartOfDay(), query.to.atTime(LocalTime.MAX))
    }

    private fun ownedTank(tankId: UUID, accountId: UUID): WaterTank =
        tankRepository.findById(tankId)?.takeIf { it.accountId == accountId } ?: throw WaterTankNotFoundException()

    private inline fun <T> tankOrInvalid(block: () -> T): T = try {
        block()
    } catch (exception: IllegalArgumentException) {
        throw InvalidWaterTankException()
    }

    private companion object {
        const val PREDICTION_HISTORY_DAYS = 7L
        const val MAX_HISTORY_DAYS = 92L
    }
}
