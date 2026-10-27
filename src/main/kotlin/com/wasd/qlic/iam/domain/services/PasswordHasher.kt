package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.model.valueobjects.PasswordHash

/**
 * Domain port to hash and verify passwords. Implemented by BCryptPasswordEncoderAdapter.
 */
interface PasswordHasher {
    fun hash(rawPassword: String): PasswordHash
    fun matches(rawPassword: String, hash: PasswordHash): Boolean
}
