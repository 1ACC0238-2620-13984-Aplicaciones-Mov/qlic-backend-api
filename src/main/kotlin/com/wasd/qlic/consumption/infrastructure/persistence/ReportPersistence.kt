package com.wasd.qlic.consumption.infrastructure.persistence

import com.wasd.qlic.consumption.domain.model.ServiceCategory
import com.wasd.qlic.consumption.domain.model.SustainabilityGoal
import com.wasd.qlic.consumption.domain.model.TariffSettings
import com.wasd.qlic.consumption.domain.repositories.SustainabilityGoalRepository
import com.wasd.qlic.consumption.domain.repositories.TariffSettingsRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

@Entity
@Table(name = "tariff_settings")
class TariffSettingsEntity(
    @Id @Column(name = "account_id") val accountId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val category: ServiceCategory,
    @Column(name = "custom_price_per_m3", precision = 10, scale = 4) val customPricePerCubicMeter: BigDecimal?,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant
)

@Entity
@Table(
    name = "sustainability_goals",
    uniqueConstraints = [UniqueConstraint(name = "uk_goal_account_month", columnNames = ["account_id", "goal_month"])]
)
class SustainabilityGoalEntity(
    @Id val id: String,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "goal_month", nullable = false, length = 7) val month: String,
    @Column(name = "reduction_percentage", nullable = false, precision = 5, scale = 2) val reductionPercentage: BigDecimal,
    @Column(name = "baseline_liters", nullable = false, precision = 14, scale = 2) val baselineLiters: BigDecimal,
    @Column(name = "updated_at", nullable = false) val updatedAt: Instant
)

@Repository
interface SpringDataTariffSettingsRepository : JpaRepository<TariffSettingsEntity, UUID>

@Repository
interface SpringDataSustainabilityGoalRepository : JpaRepository<SustainabilityGoalEntity, String>

@Component
class JpaTariffSettingsRepositoryImpl(
    private val jpa: SpringDataTariffSettingsRepository
) : TariffSettingsRepository {

    override fun save(settings: TariffSettings): TariffSettings = toDomain(
        jpa.save(TariffSettingsEntity(settings.accountId, settings.category, settings.customPricePerCubicMeter, settings.updatedAt))
    )

    override fun findByAccountId(accountId: UUID): TariffSettings? = jpa.findById(accountId).orElse(null)?.let { toDomain(it) }

    private fun toDomain(entity: TariffSettingsEntity) =
        TariffSettings(entity.accountId, entity.category, entity.customPricePerCubicMeter, entity.updatedAt)
}

/** One goal per account and month: the identifier combines both. */
@Component
class JpaSustainabilityGoalRepositoryImpl(
    private val jpa: SpringDataSustainabilityGoalRepository
) : SustainabilityGoalRepository {

    override fun save(goal: SustainabilityGoal): SustainabilityGoal = toDomain(
        jpa.save(
            SustainabilityGoalEntity(
                id = idOf(goal.accountId, goal.month),
                accountId = goal.accountId,
                month = goal.month.toString(),
                reductionPercentage = goal.reductionPercentage,
                baselineLiters = goal.baselineLiters,
                updatedAt = goal.updatedAt
            )
        )
    )

    override fun findByAccountIdAndMonth(accountId: UUID, month: YearMonth): SustainabilityGoal? =
        jpa.findById(idOf(accountId, month)).orElse(null)?.let { toDomain(it) }

    private fun idOf(accountId: UUID, month: YearMonth) = "$accountId:$month"

    private fun toDomain(entity: SustainabilityGoalEntity) = SustainabilityGoal(
        accountId = entity.accountId,
        month = YearMonth.parse(entity.month),
        reductionPercentage = entity.reductionPercentage,
        baselineLiters = entity.baselineLiters,
        updatedAt = entity.updatedAt
    )
}
