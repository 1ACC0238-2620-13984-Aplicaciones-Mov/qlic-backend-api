package com.wasd.qlic.iam.infrastructure.persistence

import com.wasd.qlic.iam.domain.model.valueobjects.InvitationState
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "invitations")
class InvitationEntity(
    @Id
    val id: UUID,

    @Column(name = "account_id", nullable = false)
    val accountId: UUID,

    @Column(name = "invited_email", nullable = false, length = 120)
    val invitedEmail: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val permission: PermissionLevel,

    @Column(name = "invited_by", nullable = false)
    val invitedBy: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var state: InvitationState,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null
)

@Entity
@Table(
    name = "authorized_users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["account_id", "subscriber_id"])]
)
class AuthorizedUserEntity(
    @Id
    val id: UUID,

    @Column(name = "account_id", nullable = false)
    val accountId: UUID,

    @Column(name = "subscriber_id", nullable = false)
    val subscriberId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val permission: PermissionLevel,

    @Column(name = "invitation_id", nullable = false)
    val invitationId: UUID,

    @Column(name = "authorized_at", nullable = false)
    val authorizedAt: Instant
)

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetTokenEntity(
    @Id
    val id: UUID,

    @Column(name = "subscriber_id", nullable = false)
    val subscriberId: UUID,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
