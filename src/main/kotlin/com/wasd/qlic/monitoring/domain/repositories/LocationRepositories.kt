package com.wasd.qlic.monitoring.domain.repositories

import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import java.util.UUID

interface LocationRepository {
    fun save(location: Location): Location
    fun findById(id: UUID): Location?
    fun findByAccountId(accountId: UUID): List<Location>
    fun delete(location: Location)
}

interface WaterPointRepository {
    fun save(waterPoint: WaterPoint): WaterPoint
    fun findById(id: UUID): WaterPoint?
    fun findByAccountId(accountId: UUID): List<WaterPoint>
    fun findByLocationId(locationId: UUID): List<WaterPoint>
    fun countByAccountId(accountId: UUID): Int
    fun existsByLocationId(locationId: UUID): Boolean
    fun delete(waterPoint: WaterPoint)
}
