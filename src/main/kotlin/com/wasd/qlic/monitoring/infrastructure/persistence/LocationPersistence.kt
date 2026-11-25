package com.wasd.qlic.monitoring.infrastructure.persistence

import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.domain.model.WaterPointType
import com.wasd.qlic.monitoring.domain.repositories.LocationRepository
import com.wasd.qlic.monitoring.domain.repositories.WaterPointRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "locations", indexes = [Index(name = "idx_locations_account", columnList = "account_id")])
class LocationEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(nullable = false, length = 80) val name: String,
    @Column(length = 160) val address: String?,
    @Column(name = "created_at", nullable = false) val createdAt: Instant
)

@Entity
@Table(
    name = "water_points",
    indexes = [
        Index(name = "idx_water_points_account", columnList = "account_id"),
        Index(name = "idx_water_points_location", columnList = "location_id")
    ]
)
class WaterPointEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "location_id", nullable = false) val locationId: UUID,
    @Column(nullable = false, length = 80) val name: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val type: WaterPointType,
    @Column(name = "created_at", nullable = false) val createdAt: Instant
)

@Repository
interface SpringDataLocationRepository : JpaRepository<LocationEntity, UUID> {
    fun findAllByAccountIdOrderByNameAsc(accountId: UUID): List<LocationEntity>
}

@Repository
interface SpringDataWaterPointRepository : JpaRepository<WaterPointEntity, UUID> {
    fun findAllByAccountIdOrderByNameAsc(accountId: UUID): List<WaterPointEntity>
    fun findAllByLocationIdOrderByNameAsc(locationId: UUID): List<WaterPointEntity>
    fun countByAccountId(accountId: UUID): Long
    fun existsByLocationId(locationId: UUID): Boolean
}

@Component
class JpaLocationRepositoryImpl(
    private val jpa: SpringDataLocationRepository
) : LocationRepository {

    override fun save(location: Location): Location = toDomain(
        jpa.save(LocationEntity(location.id, location.accountId, location.name, location.address, location.createdAt))
    )

    override fun findById(id: UUID): Location? = jpa.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<Location> =
        jpa.findAllByAccountIdOrderByNameAsc(accountId).map { toDomain(it) }

    override fun delete(location: Location) = jpa.deleteById(location.id)

    private fun toDomain(entity: LocationEntity) = Location(
        id = entity.id,
        accountId = entity.accountId,
        name = entity.name,
        address = entity.address,
        createdAt = entity.createdAt
    )
}

@Component
class JpaWaterPointRepositoryImpl(
    private val jpa: SpringDataWaterPointRepository
) : WaterPointRepository {

    override fun save(waterPoint: WaterPoint): WaterPoint = toDomain(
        jpa.save(
            WaterPointEntity(
                waterPoint.id, waterPoint.accountId, waterPoint.locationId, waterPoint.name, waterPoint.type, waterPoint.createdAt
            )
        )
    )

    override fun findById(id: UUID): WaterPoint? = jpa.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<WaterPoint> =
        jpa.findAllByAccountIdOrderByNameAsc(accountId).map { toDomain(it) }

    override fun findByLocationId(locationId: UUID): List<WaterPoint> =
        jpa.findAllByLocationIdOrderByNameAsc(locationId).map { toDomain(it) }

    override fun countByAccountId(accountId: UUID): Int = jpa.countByAccountId(accountId).toInt()

    override fun existsByLocationId(locationId: UUID): Boolean = jpa.existsByLocationId(locationId)

    override fun delete(waterPoint: WaterPoint) = jpa.deleteById(waterPoint.id)

    private fun toDomain(entity: WaterPointEntity) = WaterPoint(
        id = entity.id,
        accountId = entity.accountId,
        locationId = entity.locationId,
        name = entity.name,
        type = entity.type,
        createdAt = entity.createdAt
    )
}
