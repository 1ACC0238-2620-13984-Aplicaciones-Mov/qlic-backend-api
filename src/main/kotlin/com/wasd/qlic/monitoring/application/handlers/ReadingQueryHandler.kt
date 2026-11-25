package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.queries.GetReadingsByPeriodQuery
import com.wasd.qlic.monitoring.domain.exceptions.InvalidReadingPeriodException
import com.wasd.qlic.monitoring.domain.exceptions.WaterPointNotFoundException
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** US13 - Scenario 1: readings history of the account, filtered by location or Water Point. */
@Component
@Transactional(readOnly = true)
class ReadingQueryHandler(
    private val readingRepository: ConsumptionReadingRepository,
    private val waterPointRepository: WaterPointRepository,
    private val locationRepository: LocationRepository
) {
    fun handle(query: GetReadingsByPeriodQuery): List<ConsumptionReading> {
        if (query.to.isBefore(query.from) || ChronoUnit.DAYS.between(query.from, query.to) > MAX_DAYS) {
            throw InvalidReadingPeriodException()
        }
        val readings = readingRepository.findByAccountIdBetween(
            query.accountId, query.from.atStartOfDay(), query.to.atTime(LocalTime.MAX)
        )
        val waterPointIds: Set<java.util.UUID>? = when {
            query.waterPointId != null -> {
                waterPointRepository.findById(query.waterPointId)?.takeIf { it.accountId == query.accountId }
                    ?: throw WaterPointNotFoundException()
                setOf(query.waterPointId)
            }
            query.locationId != null -> {
                ownedLocation(locationRepository, query.locationId, query.accountId)
                waterPointRepository.findByLocationId(query.locationId).map { it.id }.toSet()
            }
            else -> null
        }
        return readings
            .filter { waterPointIds == null || it.waterPointId in waterPointIds }
            .sortedByDescending { it.recordedAt }
    }

    private companion object {
        const val MAX_DAYS = 92L
    }
}
