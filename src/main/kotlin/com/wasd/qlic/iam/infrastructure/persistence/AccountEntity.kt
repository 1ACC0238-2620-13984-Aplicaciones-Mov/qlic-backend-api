package com.wasd.qlic.iam.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "accounts")
class AccountEntity(
    @Id
    val id: UUID,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
