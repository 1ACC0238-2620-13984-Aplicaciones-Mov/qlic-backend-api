package com.wasd.qlic.alerting.domain.repositories

import com.wasd.qlic.alerting.domain.model.AlertState
import com.wasd.qlic.alerting.domain.model.LeakAlert
import java.util.UUID

interface LeakAlertRepository {
    fun save(alert: LeakAlert): LeakAlert
    fun findById(id: UUID): LeakAlert?
    fun findByAccountId(accountId: UUID): List<LeakAlert>
    fun findActiveByWaterPointId(waterPointId: UUID): LeakAlert?
}