package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.AuthenticateCommand
import com.wasd.qlic.iam.domain.exceptions.InvalidCredentialsException
import com.wasd.qlic.iam.domain.exceptions.InvalidEmailException
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.AuthenticationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Result of the authentication: subscriber, account and issued access token. */
data class AuthenticatedSession(
    val subscriber: Subscriber,
    val account: Account,
    val accessToken: AccessToken
)

/**
 * US06: invokes AuthenticationService and returns the issued token or rejects the request.
 */
@Component
class AuthenticateCommandHandler(
    private val subscriberRepository: SubscriberRepository,
    private val accountRepository: AccountRepository,
    private val authenticationService: AuthenticationService
) {
    @Transactional(readOnly = true)
    fun handle(command: AuthenticateCommand): AuthenticatedSession {
        val email = try {
            EmailAddress.of(command.email)
        } catch (exception: InvalidEmailException) {
            throw InvalidCredentialsException()
        }

        val subscriber = subscriberRepository.findByEmail(email)
        val account = subscriber?.let { accountRepository.findByOwnerId(it.id) }
        val token = authenticationService.authenticate(subscriber, command.password, account)

        return AuthenticatedSession(subscriber!!, account!!, token)
    }
}
