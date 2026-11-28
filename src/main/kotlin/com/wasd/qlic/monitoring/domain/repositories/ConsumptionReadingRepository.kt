package com.wasd.qlic.monitoring.domain.repositories

import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import java.time.LocalDateTime
import java.util.UUID

interface ConsumptionReadingRepository {
    fun save(reading: ConsumptionReading): ConsumptionReading
    /** Last reading of a Water Point registered before the given moment, or null. */
    fun findLatestByWaterPointIdBefore(waterPointId: UUID, before: LocalDateTime): ConsumptionReading?
    fun findByAccountIdBetween(accountId: UUID, from: LocalDateTime, to: LocalDateTime): List<ConsumptionReading>
}
