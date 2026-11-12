package com.wasd.qlic.monitoring.domain.repositories

import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import java.time.LocalDateTime
import java.util.UUID

interface ConsumptionReadingRepository {
    fun save(reading: ConsumptionReading): ConsumptionReading
    fun findByAccountIdBetween(accountId: UUID, from: LocalDateTime, to: LocalDateTime): List<ConsumptionReading>
}
