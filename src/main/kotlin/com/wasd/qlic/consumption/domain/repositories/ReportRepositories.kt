package com.wasd.qlic.consumption.domain.repositories

import com.wasd.qlic.consumption.domain.model.SustainabilityGoal
import com.wasd.qlic.consumption.domain.model.TariffSettings
import java.time.YearMonth
import java.util.UUID

interface TariffSettingsRepository {
    fun save(settings: TariffSettings): TariffSettings
    fun findByAccountId(accountId: UUID): TariffSettings?
}

interface SustainabilityGoalRepository {
    fun save(goal: SustainabilityGoal): SustainabilityGoal
    fun findByAccountIdAndMonth(accountId: UUID, month: YearMonth): SustainabilityGoal?
}
