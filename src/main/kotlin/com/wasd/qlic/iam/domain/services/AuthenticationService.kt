package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.exceptions.InvalidCredentialsException
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import org.springframework.stereotype.Service

/**
 * Domain Service: validates the presented credentials and issues the access token (US06),
 * including the permission level over the account (US08).
 */
@Service
class AuthenticationService(
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider
) {
    fun verifyCredentials(subscriber: Subscriber?, rawPassword: String): Subscriber {
        if (subscriber == null || !passwordHasher.matches(rawPassword, subscriber.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return subscriber
    }

    fun authenticate(
        subscriber: Subscriber?,
        rawPassword: String,
        account: Account?,
        permission: PermissionLevel = PermissionLevel.OWNER
    ): AccessToken {
        val verified = verifyCredentials(subscriber, rawPassword)
        if (account == null) throw InvalidCredentialsException()
        return tokenProvider.issue(verified, account, permission)
    }
}
