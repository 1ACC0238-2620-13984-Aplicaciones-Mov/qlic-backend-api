package com.wasd.qlic.iam.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "subscribers")
class SubscriberEntity(
    @Id
    val id: UUID,

    @Column(nullable = false, unique = true, length = 120)
    val email: String,

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,

    @Column(name = "full_name", nullable = false, length = 120)
    var fullName: String,

    @Column(length = 20)
    var phone: String? = null,

    @Column(name = "terms_accepted_at", nullable = false)
    val termsAcceptedAt: Instant,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant
)
