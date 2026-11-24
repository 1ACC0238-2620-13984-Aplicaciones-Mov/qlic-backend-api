package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.exceptions.AuthorizedUsersLimitReachedException
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.InvitationRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Domain Service: issues invitations respecting the limit of additional users of the plan (US08).
 * Authorized users and pending invitations both count against the limit.
 */
@Service
class InvitationService(
    private val planEntitlementProvider: PlanEntitlementProvider,
    private val authorizedUserRepository: AuthorizedUserRepository,
    private val invitationRepository: InvitationRepository
) {
    fun ensureCapacity(accountId: UUID, now: Instant = Instant.now()) {
        val limit = planEntitlementProvider.maxAuthorizedUsers(accountId)
        val authorized = authorizedUserRepository.findByAccountId(accountId).size
        val pending = invitationRepository.findByAccountId(accountId).count { it.isPending(now) }
        if (authorized + pending >= limit) throw AuthorizedUsersLimitReachedException(limit)
    }
}
