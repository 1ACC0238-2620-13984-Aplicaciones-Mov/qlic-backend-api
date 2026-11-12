package com.wasd.qlic.monitoring.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity: volume of water measured by an IoT device for its Water Point at a moment.
 * The readings feed the reports of Consumption Analytics (US24).
 */
class ConsumptionReading(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val deviceId: UUID,
    val waterPointId: UUID,
    val volumeLiters: BigDecimal,
    val recordedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(volumeLiters.signum() >= 0) { "The volume cannot be negative" }
    }
}
