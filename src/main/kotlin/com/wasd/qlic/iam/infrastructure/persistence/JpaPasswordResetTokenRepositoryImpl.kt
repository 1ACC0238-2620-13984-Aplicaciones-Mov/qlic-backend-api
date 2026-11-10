package com.wasd.qlic.iam.infrastructure.persistence

import com.wasd.qlic.iam.domain.model.entities.PasswordResetToken
import com.wasd.qlic.iam.domain.repositories.PasswordResetTokenRepository
import org.springframework.stereotype.Component

@Component
class JpaPasswordResetTokenRepositoryImpl(
    private val jpaRepository: SpringDataPasswordResetTokenRepository
) : PasswordResetTokenRepository {

    override fun save(token: PasswordResetToken): PasswordResetToken =
        toDomain(
            jpaRepository.save(
                PasswordResetTokenEntity(
                    id = token.id,
                    subscriberId = token.subscriberId,
                    tokenHash = token.tokenHash,
                    expiresAt = token.expiresAt,
                    usedAt = token.usedAt,
                    createdAt = token.createdAt
                )
            )
        )

    override fun findByTokenHash(tokenHash: String): PasswordResetToken? =
        jpaRepository.findByTokenHash(tokenHash)?.let { toDomain(it) }

    private fun toDomain(entity: PasswordResetTokenEntity) = PasswordResetToken(
        id = entity.id,
        subscriberId = entity.subscriberId,
        tokenHash = entity.tokenHash,
        expiresAt = entity.expiresAt,
        usedAt = entity.usedAt,
        createdAt = entity.createdAt
    )
}
