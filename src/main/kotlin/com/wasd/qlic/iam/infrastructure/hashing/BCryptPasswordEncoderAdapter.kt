package com.wasd.qlic.iam.infrastructure.hashing

import com.wasd.qlic.iam.domain.model.valueobjects.PasswordHash
import com.wasd.qlic.iam.domain.services.PasswordHasher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Adapter: hashes and verifies passwords with BCrypt (Spring Security PasswordEncoder).
 */
@Component
class BCryptPasswordEncoderAdapter(
    private val passwordEncoder: PasswordEncoder
) : PasswordHasher {

    override fun hash(rawPassword: String): PasswordHash =
        PasswordHash(requireNotNull(passwordEncoder.encode(rawPassword)))

    override fun matches(rawPassword: String, hash: PasswordHash): Boolean =
        passwordEncoder.matches(rawPassword, hash.value)
}
