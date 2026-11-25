package com.wasd.qlic.monitoring.application.commands

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** Ingestion of a consumption reading sent by an IoT device, with its optional telemetry. */
data class RegisterConsumptionReadingCommand(
    val accountId: UUID,
    val deviceId: UUID,
    val volumeLiters: BigDecimal,
    val recordedAt: LocalDateTime?,
    val flowRateLitersPerMinute: BigDecimal? = null,
    val pressureBar: BigDecimal? = null,
    val batteryPercentage: Int? = null
)
