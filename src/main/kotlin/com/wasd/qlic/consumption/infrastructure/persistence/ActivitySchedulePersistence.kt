package com.wasd.qlic.consumption.infrastructure.persistence

import com.wasd.qlic.consumption.domain.model.ActivitySchedule
import com.wasd.qlic.consumption.domain.model.PremisesActivitySchedule
import com.wasd.qlic.consumption.domain.repositories.ActivityScheduleRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "activity_schedules")
class ActivityScheduleEntity(
    @Id @Column(name = "account_id") val accountId: UUID,
    @Column(name = "opening_time", nullable = false) val openingTime: LocalTime,
    @Column(name = "closing_time", nullable = false) val closingTime: LocalTime,
    @Column(name = "active_days", nullable = false, length = 70) val activeDays: String,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant
)

@Repository
interface SpringDataActivityScheduleRepository : JpaRepository<ActivityScheduleEntity, UUID>

@Component
class JpaActivityScheduleRepositoryImpl(
    private val jpa: SpringDataActivityScheduleRepository
) : ActivityScheduleRepository {

    override fun save(schedule: PremisesActivitySchedule): PremisesActivitySchedule = toDomain(
        jpa.save(
            ActivityScheduleEntity(
                accountId = schedule.accountId,
                openingTime = schedule.schedule.openingTime,
                closingTime = schedule.schedule.closingTime,
                activeDays = schedule.schedule.activeDays.sorted().joinToString(",") { it.name },
                updatedAt = schedule.updatedAt
            )
        )
    )

    override fun findByAccountId(accountId: UUID): PremisesActivitySchedule? =
        jpa.findById(accountId).orElse(null)?.let { toDomain(it) }

    private fun toDomain(entity: ActivityScheduleEntity) = PremisesActivitySchedule(
        accountId = entity.accountId,
        schedule = ActivitySchedule(
            openingTime = entity.openingTime,
            closingTime = entity.closingTime,
            activeDays = entity.activeDays.split(",").filter { it.isNotBlank() }.map { DayOfWeek.valueOf(it) }.toSet()
        ),
        updatedAt = entity.updatedAt
    )
}
