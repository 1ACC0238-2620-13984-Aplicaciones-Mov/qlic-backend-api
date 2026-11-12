package com.wasd.qlic.iam.domain.model.valueobjects

/**
 * Value Object: the hashed password. It never holds plain text.
 */
data class PasswordHash(val value: String) {
    init {
        require(value.isNotBlank()) { "Password hash must not be blank" }
    }
}
