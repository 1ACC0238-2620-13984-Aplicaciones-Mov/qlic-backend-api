package com.wasd.qlic.consumption.infrastructure.acl

import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.ReadingSnapshot
import com.wasd.qlic.consumption.domain.services.ConsumptionReadingProvider
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import org.springframework.stereotype.Component
import java.util.UUID

/** Anti-corruption layer with Device Monitoring: translates its readings into ReadingSnapshot (US24). */
@Component
class ConsumptionReadingAdapter(
    private val readingRepository: ConsumptionReadingRepository
) : ConsumptionReadingProvider {
    override fun readings(accountId: UUID, period: Period): List<ReadingSnapshot> =
        readingRepository.findByAccountIdBetween(accountId, period.start(), period.end())
            .map { ReadingSnapshot(it.waterPointId, it.volumeLiters, it.recordedAt) }
}
