package com.wasd.qlic.support.infrastructure.notifications

import com.wasd.qlic.support.domain.services.SupportNotificationSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

/**
 * Adapter: notifies the subscriber about the technical visit (US36 - Scenario 2). No push provider is
 * contracted yet, so the notification is written to the application log; the app reads the
 * assignment from GET /api/v1/incidents.
 */
@Component
class SupportNotificationAdapter : SupportNotificationSender {

    private val logger = LoggerFactory.getLogger(SupportNotificationAdapter::class.java)

    override fun notifyTechnicianAssigned(accountId: UUID, incidentId: UUID, technicianName: String, scheduledAt: LocalDateTime) {
        logger.info(
            "[notification] Account: {} | Incident {}: technician {} assigned, visit scheduled for {}",
            accountId, incidentId, technicianName, scheduledAt
        )
    }
}
