package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.exceptions.InvalidCredentialsException
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import org.springframework.stereotype.Service

/**
 * Domain Service: validates the presented credentials and issues the access token (US06).
 */
@Service
class AuthenticationService(
    private val passwordHasher: PasswordHasher,
    private val tokenProvider: TokenProvider
) {
    fun authenticate(subscriber: Subscriber?, rawPassword: String, account: Account?): AccessToken {
        if (subscriber == null || account == null ||
            !passwordHasher.matches(rawPassword, subscriber.passwordHash)
        ) {
            throw InvalidCredentialsException()
        }
        return tokenProvider.issue(subscriber, account)
    }
}
