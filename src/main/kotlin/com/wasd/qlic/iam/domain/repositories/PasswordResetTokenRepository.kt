package com.wasd.qlic.iam.domain.repositories

import com.wasd.qlic.iam.domain.model.entities.PasswordResetToken

interface PasswordResetTokenRepository {
    fun save(token: PasswordResetToken): PasswordResetToken
    fun findByTokenHash(tokenHash: String): PasswordResetToken?
}
