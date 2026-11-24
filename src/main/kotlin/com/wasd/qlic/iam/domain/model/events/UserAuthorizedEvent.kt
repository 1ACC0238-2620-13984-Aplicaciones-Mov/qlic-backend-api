package com.wasd.qlic.iam.domain.model.events

import java.time.Instant
import java.util.UUID

/**
 * Domain Event published when an invitation is accepted and a user gains access to an account (US08).
 */
data class UserAuthorizedEvent(
    val accountId: UUID,
    val subscriberId: UUID,
    val permission: String,
    val occurredOn: Instant = Instant.now()
)
