package com.wasd.qlic.shared.interfaces.rest.security

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

/**
 * Reads the identity of the requester from the validated access token (TS01), so that
 * every bounded context works on the account of the token instead of an id sent by the client.
 */
fun Jwt.subscriberId(): UUID = UUID.fromString(requireNotNull(subject) { "The token has no subject" })

fun Jwt.accountId(): UUID = UUID.fromString(requireNotNull(getClaimAsString("accountId")) { "The token has no account" })

/** Permission level over the account of the token. Tokens without the claim belong to the owner. */
fun Jwt.permissionName(): String = getClaimAsString("permission") ?: "OWNER"
