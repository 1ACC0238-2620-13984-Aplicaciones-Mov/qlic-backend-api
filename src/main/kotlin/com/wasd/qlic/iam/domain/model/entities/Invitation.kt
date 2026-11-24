package com.wasd.qlic.iam.domain.model.entities

import com.wasd.qlic.iam.domain.exceptions.InvitationExpiredException
import com.wasd.qlic.iam.domain.exceptions.InvitationNotAddressedToSubscriberException
import com.wasd.qlic.iam.domain.exceptions.InvitationNotPendingException
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.InvitationState
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Entity: invitation sent to a person to access an account with a permission level (US08).
 */
class Invitation(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val invitedEmail: EmailAddress,
    val permission: PermissionLevel,
    val invitedBy: UUID,
    var state: InvitationState = InvitationState.PENDING,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant = createdAt.plus(VALIDITY),
    var acceptedAt: Instant? = null
) {
    init {
        require(permission != PermissionLevel.OWNER) { "The owner permission cannot be delegated" }
    }

    fun isPending(now: Instant = Instant.now()): Boolean =
        state == InvitationState.PENDING && now.isBefore(expiresAt)

    /** Accepts the invitation and returns the new authorized user of the account. */
    fun accept(subscriberId: UUID, subscriberEmail: EmailAddress, now: Instant = Instant.now()): AuthorizedUser {
        if (subscriberEmail != invitedEmail) throw InvitationNotAddressedToSubscriberException()
        if (state != InvitationState.PENDING) throw InvitationNotPendingException()
        if (!now.isBefore(expiresAt)) {
            state = InvitationState.EXPIRED
            throw InvitationExpiredException()
        }
        state = InvitationState.ACCEPTED
        acceptedAt = now
        return AuthorizedUser(
            accountId = accountId,
            subscriberId = subscriberId,
            permission = permission,
            invitationId = id,
            authorizedAt = now
        )
    }

    companion object {
        val VALIDITY: Duration = Duration.ofDays(7)
    }
}
