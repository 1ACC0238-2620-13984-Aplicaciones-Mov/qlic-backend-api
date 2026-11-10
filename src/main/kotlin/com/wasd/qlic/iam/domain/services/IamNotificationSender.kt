package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import java.time.Instant

/**
 * Domain port to notify people by email: password reset links (US07) and access invitations (US08).
 */
interface IamNotificationSender {
    fun sendPasswordResetLink(email: EmailAddress, resetLink: String, expiresAt: Instant)
    fun sendInvitation(email: EmailAddress, accountName: String, permission: String, expiresAt: Instant)
}
