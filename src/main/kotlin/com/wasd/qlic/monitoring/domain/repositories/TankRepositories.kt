package com.wasd.qlic.monitoring.domain.repositories

import com.wasd.qlic.monitoring.domain.model.TankLevelReading
import com.wasd.qlic.monitoring.domain.model.WaterTank
import java.time.LocalDateTime
import java.util.UUID

interface WaterTankRepository {
    fun save(tank: WaterTank): WaterTank
    fun findById(id: UUID): WaterTank?
    fun findByAccountId(accountId: UUID): List<WaterTank>
    fun existsByLocationId(locationId: UUID): Boolean
    fun delete(tank: WaterTank)
}

interface TankLevelReadingRepository {
    fun save(reading: TankLevelReading): TankLevelReading
    fun findByTankIdBetween(tankId: UUID, from: LocalDateTime, to: LocalDateTime): List<TankLevelReading>
    fun deleteByTankId(tankId: UUID)
}
