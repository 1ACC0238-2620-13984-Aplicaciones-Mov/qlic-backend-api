package com.wasd.qlic.iam.domain.model.entities

import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.PasswordHash
import java.time.Instant
import java.util.UUID

/**
 * Entity: account holder, with credentials and profile data.
 */
class Subscriber(
    val id: UUID = UUID.randomUUID(),
    val email: EmailAddress,
    var passwordHash: PasswordHash,
    var fullName: String,
    var phone: String? = null,
    val termsAcceptedAt: Instant,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(fullName.isNotBlank()) { "Full name is required" }
    }
}
