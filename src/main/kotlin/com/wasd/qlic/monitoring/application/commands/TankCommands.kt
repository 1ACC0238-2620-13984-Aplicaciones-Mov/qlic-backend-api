package com.wasd.qlic.monitoring.application.commands

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** US28: registers a tank of a location, optionally monitored by a device of the account. */
data class SaveWaterTankCommand(
    val accountId: UUID,
    val locationId: UUID,
    val name: String,
    val capacityLiters: BigDecimal,
    val criticalLevelPercentage: BigDecimal,
    val deviceId: UUID?,
    val tankId: UUID? = null
)

data class DeleteWaterTankCommand(val tankId: UUID, val accountId: UUID)

/** US28, US29: level reported by the device of the tank. */
data class RegisterTankLevelCommand(
    val tankId: UUID,
    val accountId: UUID,
    val levelPercentage: BigDecimal,
    val recordedAt: LocalDateTime?
)
