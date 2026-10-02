package com.wasd.qlic.alerting.infrastructure.persistence

import com.wasd.qlic.alerting.domain.model.AlertState
import com.wasd.qlic.alerting.domain.model.LeakAlert
import com.wasd.qlic.alerting.domain.repositories.LeakAlertRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaLeakAlertRepositoryImpl(
    private val jpaRepository: SpringDataLeakAlertRepository
) : LeakAlertRepository {

    override fun save(alert: LeakAlert): LeakAlert {
        val entity = LeakAlertEntity(
            id = alert.id,
            accountId = alert.accountId,
            waterPointId = alert.waterPointId,
            state = alert.state,
            urgency = alert.urgency,
            detectedAt = alert.detectedAt,
            estimatedVolume = alert.estimatedVolumeLiters,
            estimatedCost = alert.estimatedCostSoles,
            recommendedAction = alert.recommendedAction,
            acknowledgedAction = alert.acknowledgedAction,
            previousAlertId = alert.previousAlertId
        )
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: UUID): LeakAlert? {
        return jpaRepository.findById(id).map { toDomain(it) }.orElse(null)
    }

    override fun findByAccountId(accountId: UUID): List<LeakAlert> {
        return jpaRepository.findByAccountId(accountId).map { toDomain(it) }
    }

    override fun findActiveByWaterPointId(waterPointId: UUID): LeakAlert? {
        val entity = jpaRepository.findFirstByWaterPointIdAndState(waterPointId, AlertState.ACTIVE)
        return entity?.let { toDomain(it) }
    }

    private fun toDomain(entity: LeakAlertEntity): LeakAlert {
        return LeakAlert(
            id = entity.id,
            accountId = entity.accountId,
            waterPointId = entity.waterPointId,
            state = entity.state,
            urgency = entity.urgency,
            detectedAt = entity.detectedAt,
            estimatedVolumeLiters = entity.estimatedVolume,
            estimatedCostSoles = entity.estimatedCost,
            recommendedAction = entity.recommendedAction,
            acknowledgedAction = entity.acknowledgedAction,
            previousAlertId = entity.previousAlertId
        )
    }
}