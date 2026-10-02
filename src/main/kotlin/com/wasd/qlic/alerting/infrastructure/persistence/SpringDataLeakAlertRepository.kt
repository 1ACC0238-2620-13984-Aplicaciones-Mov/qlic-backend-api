package com.wasd.qlic.alerting.infrastructure.persistence

import com.wasd.qlic.alerting.domain.model.AlertState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataLeakAlertRepository : JpaRepository<LeakAlertEntity, UUID> {
    fun findByAccountId(accountId: UUID): List<LeakAlertEntity>
    fun findFirstByWaterPointIdAndState(waterPointId: UUID, state: AlertState): LeakAlertEntity?
}