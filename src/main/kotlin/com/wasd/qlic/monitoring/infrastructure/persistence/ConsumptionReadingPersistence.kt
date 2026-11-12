package com.wasd.qlic.monitoring.infrastructure.persistence

import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "consumption_readings",
    indexes = [Index(name = "idx_readings_account_recorded", columnList = "account_id, recorded_at")]
)
class ConsumptionReadingEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "device_id", nullable = false) val deviceId: UUID,
    @Column(name = "water_point_id", nullable = false) val waterPointId: UUID,
    @Column(name = "volume_liters", nullable = false, precision = 12, scale = 3) val volumeLiters: BigDecimal,
    @Column(name = "recorded_at", nullable = false) val recordedAt: LocalDateTime
)

@Repository
interface SpringDataConsumptionReadingRepository : JpaRepository<ConsumptionReadingEntity, UUID> {
    fun findAllByAccountIdAndRecordedAtBetween(accountId: UUID, from: LocalDateTime, to: LocalDateTime): List<ConsumptionReadingEntity>
}

@Component
class JpaConsumptionReadingRepositoryImpl(
    private val jpa: SpringDataConsumptionReadingRepository
) : ConsumptionReadingRepository {

    override fun save(reading: ConsumptionReading): ConsumptionReading = toDomain(
        jpa.save(
            ConsumptionReadingEntity(
                reading.id, reading.accountId, reading.deviceId, reading.waterPointId, reading.volumeLiters, reading.recordedAt
            )
        )
    )

    override fun findByAccountIdBetween(accountId: UUID, from: LocalDateTime, to: LocalDateTime): List<ConsumptionReading> =
        jpa.findAllByAccountIdAndRecordedAtBetween(accountId, from, to).map { toDomain(it) }

    private fun toDomain(entity: ConsumptionReadingEntity) = ConsumptionReading(
        id = entity.id,
        accountId = entity.accountId,
        deviceId = entity.deviceId,
        waterPointId = entity.waterPointId,
        volumeLiters = entity.volumeLiters,
        recordedAt = entity.recordedAt
    )
}
