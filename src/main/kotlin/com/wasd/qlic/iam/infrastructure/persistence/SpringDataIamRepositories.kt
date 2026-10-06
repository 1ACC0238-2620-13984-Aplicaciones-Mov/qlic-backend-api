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
