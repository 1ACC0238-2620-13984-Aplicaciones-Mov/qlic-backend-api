package com.wasd.qlic.iam.domain.model.entities

import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import java.time.Instant
import java.util.UUID

/**
 * Entity: person with delegated access to an account and its permission level (US08).
 */
class AuthorizedUser(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val subscriberId: UUID,
    val permission: PermissionLevel,
    val invitationId: UUID,
    val authorizedAt: Instant = Instant.now()
) {
    init {
        require(permission != PermissionLevel.OWNER) { "An authorized user cannot be the owner" }
    }
}
