package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.PasswordHash
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.PasswordHasher
import com.wasd.qlic.iam.domain.services.TokenProvider
import java.time.Instant
import java.util.UUID

class InMemorySubscriberRepository : SubscriberRepository {
    private val storage = mutableMapOf<UUID, Subscriber>()
    override fun save(subscriber: Subscriber): Subscriber = subscriber.also { storage[it.id] = it }
    override fun findByEmail(email: EmailAddress): Subscriber? = storage.values.firstOrNull { it.email == email }
    override fun existsByEmail(email: EmailAddress): Boolean = findByEmail(email) != null
    fun count(): Int = storage.size
}

class InMemoryAccountRepository : AccountRepository {
    private val storage = mutableMapOf<UUID, Account>()
    override fun save(account: Account): Account = account.also { storage[it.id] = it }
    override fun findByOwnerId(ownerId: UUID): Account? = storage.values.firstOrNull { it.ownerId == ownerId }
    fun count(): Int = storage.size
}

/** Deterministic hasher for tests: does not use BCrypt, only adds a prefix. */
class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String) = PasswordHash("hashed:$rawPassword")
    override fun matches(rawPassword: String, hash: PasswordHash) = hash.value == "hashed:$rawPassword"
}

class FakeTokenProvider : TokenProvider {
    override fun issue(subscriber: Subscriber, account: Account) =
        AccessToken(value = "token-${subscriber.id}", expiresAt = Instant.now().plusSeconds(3600))
}
