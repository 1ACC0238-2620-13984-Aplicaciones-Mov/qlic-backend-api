package com.wasd.qlic.iam.infrastructure.notifications

import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.services.IamNotificationSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Adapter: delivers the IAM emails (US07, US08). No email provider is contracted yet, so the
 * messages are written to the application log; replacing this class does not affect the domain.
 */
@Component
class EmailNotificationAdapter : IamNotificationSender {

    private val logger = LoggerFactory.getLogger(EmailNotificationAdapter::class.java)

    override fun sendPasswordResetLink(email: EmailAddress, resetLink: String, expiresAt: Instant) {
        logger.info("[email] To: {} | Qlic password reset | Link: {} | Valid until: {}", email.value, resetLink, expiresAt)
    }

    override fun sendInvitation(email: EmailAddress, accountName: String, permission: String, expiresAt: Instant) {
        logger.info(
            "[email] To: {} | You were invited to the Qlic account '{}' with {} permission | Valid until: {}",
            email.value, accountName, permission, expiresAt
        )
    }
}
