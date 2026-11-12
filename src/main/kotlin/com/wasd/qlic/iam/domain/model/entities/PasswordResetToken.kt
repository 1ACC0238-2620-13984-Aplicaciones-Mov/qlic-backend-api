package com.wasd.qlic.iam.domain.model.entities

import com.wasd.qlic.iam.domain.exceptions.ResetTokenExpiredException
import com.wasd.qlic.iam.domain.exceptions.ResetTokenInvalidException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

/**
 * Entity: single-use password reset token with limited validity (US07).
 * Only the SHA-256 hash of the token is stored; the plain value travels in the reset link.
 */
class PasswordResetToken(
    val id: UUID = UUID.randomUUID(),
    val subscriberId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    var usedAt: Instant? = null,
    val createdAt: Instant = Instant.now()
) {
    /** US07 - Scenario 2: an expired or already used token is rejected. */
    fun consume(now: Instant = Instant.now()) {
        if (usedAt != null) throw ResetTokenInvalidException()
        if (!now.isBefore(expiresAt)) throw ResetTokenExpiredException()
        usedAt = now
    }

    companion object {
        private val random = SecureRandom()

        fun generatePlainToken(): String {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        fun hash(plainToken: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(plainToken.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
