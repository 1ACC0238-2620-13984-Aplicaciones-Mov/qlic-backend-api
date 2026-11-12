package com.wasd.qlic.consumption.domain.services

import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.ReadingSnapshot
import java.util.UUID

/** Domain port: readings of an account in a period, provided by Device Monitoring. */
interface ConsumptionReadingProvider {
    fun readings(accountId: UUID, period: Period): List<ReadingSnapshot>
}
