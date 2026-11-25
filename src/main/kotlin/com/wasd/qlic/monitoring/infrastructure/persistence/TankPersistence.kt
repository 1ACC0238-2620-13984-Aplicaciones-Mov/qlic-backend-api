package com.wasd.qlic.monitoring.infrastructure.persistence

import com.wasd.qlic.monitoring.domain.model.TankLevelReading
import com.wasd.qlic.monitoring.domain.model.WaterTank
import com.wasd.qlic.monitoring.domain.repositories.TankLevelReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterTankRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "water_tanks", indexes = [Index(name = "idx_water_tanks_account", columnList = "account_id")])
class WaterTankEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "location_id", nullable = false) val locationId: UUID,
    @Column(nullable = false, length = 80) val name: String,
    @Column(name = "capacity_liters", nullable = false, precision = 12, scale = 2) val capacityLiters: BigDecimal,
    @Column(name = "critical_level_percentage", nullable = false, precision = 5, scale = 2) val criticalLevelPercentage: BigDecimal,
    @Column(name = "device_id") val deviceId: UUID?,
    @Column(name = "current_level_percentage", precision = 5, scale = 2) val currentLevelPercentage: BigDecimal?,
    @Column(name = "last_reading_at") val lastReadingAt: LocalDateTime?,
    @Column(name = "low_level_alert_active", nullable = false) val lowLevelAlertActive: Boolean,
    @Column(name = "created_at", nullable = false) val createdAt: Instant
)

@Entity
@Table(
    name = "tank_level_readings",
    indexes = [Index(name = "idx_tank_levels_tank_recorded", columnList = "tank_id, recorded_at")]
)
class TankLevelReadingEntity(
    @Id val id: UUID,
    @Column(name = "tank_id", nullable = false) val tankId: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "level_percentage", nullable = false, precision = 5, scale = 2) val levelPercentage: BigDecimal,
    @Column(name = "recorded_at", nullable = false) val recordedAt: LocalDateTime
)

@Repository
interface SpringDataWaterTankRepository : JpaRepository<WaterTankEntity, UUID> {
    fun findAllByAccountIdOrderByNameAsc(accountId: UUID): List<WaterTankEntity>
    fun existsByLocationId(locationId: UUID): Boolean
}

@Repository
interface SpringDataTankLevelReadingRepository : JpaRepository<TankLevelReadingEntity, UUID> {
    fun findAllByTankIdAndRecordedAtBetweenOrderByRecordedAtAsc(
        tankId: UUID, from: LocalDateTime, to: LocalDateTime
    ): List<TankLevelReadingEntity>

    @Modifying
    @Query("delete from TankLevelReadingEntity r where r.tankId = :tankId")
    fun deleteAllByTankId(@Param("tankId") tankId: UUID)
}

@Component
class JpaWaterTankRepositoryImpl(
    private val jpa: SpringDataWaterTankRepository
) : WaterTankRepository {

    override fun save(tank: WaterTank): WaterTank = toDomain(
        jpa.save(
            WaterTankEntity(
                id = tank.id,
                accountId = tank.accountId,
                locationId = tank.locationId,
                name = tank.name,
                capacityLiters = tank.capacityLiters,
                criticalLevelPercentage = tank.criticalLevelPercentage,
                deviceId = tank.deviceId,
                currentLevelPercentage = tank.currentLevelPercentage,
                lastReadingAt = tank.lastReadingAt,
                lowLevelAlertActive = tank.lowLevelAlertActive,
                createdAt = tank.createdAt
            )
        )
    )

    override fun findById(id: UUID): WaterTank? = jpa.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<WaterTank> =
        jpa.findAllByAccountIdOrderByNameAsc(accountId).map { toDomain(it) }

    override fun existsByLocationId(locationId: UUID): Boolean = jpa.existsByLocationId(locationId)

    override fun delete(tank: WaterTank) = jpa.deleteById(tank.id)

    private fun toDomain(entity: WaterTankEntity) = WaterTank(
        id = entity.id,
        accountId = entity.accountId,
        locationId = entity.locationId,
        name = entity.name,
        capacityLiters = entity.capacityLiters,
        criticalLevelPercentage = entity.criticalLevelPercentage,
        deviceId = entity.deviceId,
        currentLevelPercentage = entity.currentLevelPercentage,
        lastReadingAt = entity.lastReadingAt,
        lowLevelAlertActive = entity.lowLevelAlertActive,
        createdAt = entity.createdAt
    )
}

@Component
class JpaTankLevelReadingRepositoryImpl(
    private val jpa: SpringDataTankLevelReadingRepository
) : TankLevelReadingRepository {

    override fun save(reading: TankLevelReading): TankLevelReading = toDomain(
        jpa.save(TankLevelReadingEntity(reading.id, reading.tankId, reading.accountId, reading.levelPercentage, reading.recordedAt))
    )

    override fun findByTankIdBetween(tankId: UUID, from: LocalDateTime, to: LocalDateTime): List<TankLevelReading> =
        jpa.findAllByTankIdAndRecordedAtBetweenOrderByRecordedAtAsc(tankId, from, to).map { toDomain(it) }

    override fun deleteByTankId(tankId: UUID) = jpa.deleteAllByTankId(tankId)

    private fun toDomain(entity: TankLevelReadingEntity) =
        TankLevelReading(entity.id, entity.tankId, entity.accountId, entity.levelPercentage, entity.recordedAt)
}
