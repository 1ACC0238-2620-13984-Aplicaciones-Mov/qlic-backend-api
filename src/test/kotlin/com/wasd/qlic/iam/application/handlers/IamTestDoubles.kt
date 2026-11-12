package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.AuthorizedUser
import com.wasd.qlic.iam.domain.model.entities.Invitation
import com.wasd.qlic.iam.domain.model.entities.PasswordResetToken
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.PasswordHash
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.InvitationRepository
import com.wasd.qlic.iam.domain.repositories.PasswordResetTokenRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.IamNotificationSender
import com.wasd.qlic.iam.domain.services.PasswordHasher
import com.wasd.qlic.iam.domain.services.PlanEntitlementProvider
import com.wasd.qlic.iam.domain.services.TokenProvider
import java.time.Instant
import java.util.UUID

class InMemorySubscriberRepository : SubscriberRepository {
    private val storage = mutableMapOf<UUID, Subscriber>()
    override fun save(subscriber: Subscriber): Subscriber = subscriber.also { storage[it.id] = it }
    override fun findById(id: UUID): Subscriber? = storage[id]
    override fun findByEmail(email: EmailAddress): Subscriber? = storage.values.firstOrNull { it.email == email }
    override fun existsByEmail(email: EmailAddress): Boolean = findByEmail(email) != null
    fun count(): Int = storage.size
}

class InMemoryAccountRepository : AccountRepository {
    private val storage = mutableMapOf<UUID, Account>()
    override fun save(account: Account): Account = account.also { storage[it.id] = it }
    override fun findById(id: UUID): Account? = storage[id]
    override fun findByOwnerId(ownerId: UUID): Account? = storage.values.firstOrNull { it.ownerId == ownerId }
    fun count(): Int = storage.size
}

class InMemoryInvitationRepository : InvitationRepository {
    private val storage = mutableMapOf<UUID, Invitation>()
    override fun save(invitation: Invitation): Invitation = invitation.also { storage[it.id] = it }
    override fun findById(id: UUID): Invitation? = storage[id]
    override fun findByAccountId(accountId: UUID): List<Invitation> = storage.values.filter { it.accountId == accountId }
    override fun findByInvitedEmail(email: EmailAddress): List<Invitation> = storage.values.filter { it.invitedEmail == email }
}

class InMemoryAuthorizedUserRepository : AuthorizedUserRepository {
    private val storage = mutableMapOf<UUID, AuthorizedUser>()
    override fun save(authorizedUser: AuthorizedUser): AuthorizedUser = authorizedUser.also { storage[it.id] = it }
    override fun findById(id: UUID): AuthorizedUser? = storage[id]
    override fun findByAccountId(accountId: UUID): List<AuthorizedUser> = storage.values.filter { it.accountId == accountId }
    override fun findBySubscriberId(subscriberId: UUID): List<AuthorizedUser> = storage.values.filter { it.subscriberId == subscriberId }
    override fun findByAccountIdAndSubscriberId(accountId: UUID, subscriberId: UUID): AuthorizedUser? =
        storage.values.firstOrNull { it.accountId == accountId && it.subscriberId == subscriberId }
    override fun delete(authorizedUser: AuthorizedUser) {
        storage.remove(authorizedUser.id)
    }
}

class InMemoryPasswordResetTokenRepository : PasswordResetTokenRepository {
    private val storage = mutableMapOf<String, PasswordResetToken>()
    override fun save(token: PasswordResetToken): PasswordResetToken = token.also { storage[it.tokenHash] = it }
    override fun findByTokenHash(tokenHash: String): PasswordResetToken? = storage[tokenHash]
    fun all(): List<PasswordResetToken> = storage.values.toList()
}

/** Records the notifications instead of sending emails. */
class RecordingNotificationSender : IamNotificationSender {
    val resetLinks = mutableListOf<Pair<String, String>>()
    val invitations = mutableListOf<String>()
    override fun sendPasswordResetLink(email: EmailAddress, resetLink: String, expiresAt: Instant) {
        resetLinks += email.value to resetLink
    }
    override fun sendInvitation(email: EmailAddress, accountName: String, permission: String, expiresAt: Instant) {
        invitations += email.value
    }
}

class FixedPlanEntitlementProvider(var limit: Int) : PlanEntitlementProvider {
    override fun maxAuthorizedUsers(accountId: UUID): Int = limit
}

/** Deterministic hasher for tests: does not use BCrypt, only adds a prefix. */
class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String) = PasswordHash("hashed:$rawPassword")
    override fun matches(rawPassword: String, hash: PasswordHash) = hash.value == "hashed:$rawPassword"
}

class FakeTokenProvider : TokenProvider {
    override fun issue(subscriber: Subscriber, account: Account, permission: PermissionLevel) =
        AccessToken(value = "token-${subscriber.id}-${permission.name}", expiresAt = Instant.now().plusSeconds(3600))
}
