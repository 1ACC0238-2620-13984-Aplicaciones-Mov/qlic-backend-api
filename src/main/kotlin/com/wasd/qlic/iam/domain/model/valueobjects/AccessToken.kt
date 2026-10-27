package com.wasd.qlic.iam.domain.model.valueobjects

import java.time.Instant

/**
 * Value Object: access token issued after a successful authentication.
 */
data class AccessToken(
    val value: String,
    val expiresAt: Instant
) {
    val type: String = "Bearer"
}
