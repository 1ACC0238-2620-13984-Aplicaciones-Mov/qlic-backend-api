package com.wasd.qlic.iam.domain.model.valueobjects

/**
 * Value Object: access level granted over an account (US08).
 * VIEWER can only consult; OPERATOR can also modify the configuration; OWNER is the account holder.
 */
enum class PermissionLevel {
    VIEWER,
    OPERATOR,
    OWNER;

    fun canModify(): Boolean = this != VIEWER

    /** OAuth2 scopes written in the access token; the API grants write operations with account:write. */
    fun scopes(): String = if (canModify()) "account:read account:write" else "account:read"
}
