package com.wasd.qlic.iam.domain.model.events

import java.time.Instant
import java.util.UUID

/**
 * Domain Event published when a new account is created (US05).
 */
data class AccountCreatedEvent(
    val accountId: UUID,
    val subscriberId: UUID,
    val email: String,
    val occurredOn: Instant = Instant.now()
)
