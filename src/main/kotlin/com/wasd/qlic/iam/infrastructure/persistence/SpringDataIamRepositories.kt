package com.wasd.qlic.iam.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataSubscriberRepository : JpaRepository<SubscriberEntity, UUID> {
    fun findByEmail(email: String): SubscriberEntity?
    fun existsByEmail(email: String): Boolean
}

@Repository
interface SpringDataAccountRepository : JpaRepository<AccountEntity, UUID> {
    fun findFirstByOwnerId(ownerId: UUID): AccountEntity?
}

@Repository
interface SpringDataInvitationRepository : JpaRepository<InvitationEntity, UUID> {
    fun findAllByAccountId(accountId: UUID): List<InvitationEntity>
    fun findAllByInvitedEmail(invitedEmail: String): List<InvitationEntity>
}

@Repository
interface SpringDataAuthorizedUserRepository : JpaRepository<AuthorizedUserEntity, UUID> {
    fun findAllByAccountId(accountId: UUID): List<AuthorizedUserEntity>
    fun findAllBySubscriberId(subscriberId: UUID): List<AuthorizedUserEntity>
    fun findFirstByAccountIdAndSubscriberId(accountId: UUID, subscriberId: UUID): AuthorizedUserEntity?
}

@Repository
interface SpringDataPasswordResetTokenRepository : JpaRepository<PasswordResetTokenEntity, UUID> {
    fun findByTokenHash(tokenHash: String): PasswordResetTokenEntity?
}
