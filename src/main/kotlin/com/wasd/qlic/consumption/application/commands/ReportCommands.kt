package com.wasd.qlic.consumption.application.commands

import com.wasd.qlic.consumption.domain.model.ServiceCategory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** US23: service category of the account and, optionally, its own price per cubic meter. */
data class DefineTariffCommand(
    val accountId: UUID,
    val category: ServiceCategory,
    val customPricePerCubicMeter: BigDecimal?
)

/** US27: reduction goal for a month, measured against the consumption of the previous month. */
data class DefineSustainabilityGoalCommand(
    val accountId: UUID,
    val month: YearMonth,
    val reductionPercentage: BigDecimal,
    val today: LocalDate = LocalDate.now()
)
