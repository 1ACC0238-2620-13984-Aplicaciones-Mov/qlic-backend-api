package com.wasd.qlic.alerting.application.handlers

import com.wasd.qlic.alerting.application.queries.GetAlertsByAccountQuery
import com.wasd.qlic.alerting.domain.model.LeakAlert
import com.wasd.qlic.alerting.domain.repositories.LeakAlertRepository
import org.springframework.stereotype.Component

@Component
class GetAlertsByAccountQueryHandler(
    private val leakAlertRepository: LeakAlertRepository
) {
    fun handle(query: GetAlertsByAccountQuery): List<LeakAlert> {
        return leakAlertRepository.findByAccountId(query.accountId)
    }
}