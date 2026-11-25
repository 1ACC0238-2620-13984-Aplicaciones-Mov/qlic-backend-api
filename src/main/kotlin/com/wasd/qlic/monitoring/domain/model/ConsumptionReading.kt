package com.wasd.qlic.monitoring.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity: volume of water measured by an IoT device for its Water Point at a moment, with the flow
 * and pressure of the line when the device reports them. The readings feed the monitoring dashboard
 * and the reports of Consumption Analytics.
 */
class ConsumptionReading(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val deviceId: UUID,
    val waterPointId: UUID,
    val volumeLiters: BigDecimal,
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    val flowRateLitersPerMinute: BigDecimal? = null,
    val pressureBar: BigDecimal? = null
) {
    init {
        require(volumeLiters.signum() >= 0) { "The volume cannot be negative" }
        require(flowRateLitersPerMinute == null || flowRateLitersPerMinute.signum() >= 0) { "The flow cannot be negative" }
        require(pressureBar == null || pressureBar.signum() >= 0) { "The pressure cannot be negative" }
    }
}
