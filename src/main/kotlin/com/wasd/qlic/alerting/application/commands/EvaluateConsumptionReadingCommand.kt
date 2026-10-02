package com.wasd.qlic.alerting.application.commands

import java.math.BigDecimal
import java.util.UUID

data class EvaluateConsumptionReadingCommand(
    val accountId: UUID,
    val waterPointId: UUID,
    val currentFlowLitersPerHour: BigDecimal,
    val thresholdLimit: BigDecimal,
    val isSustained: Boolean
)