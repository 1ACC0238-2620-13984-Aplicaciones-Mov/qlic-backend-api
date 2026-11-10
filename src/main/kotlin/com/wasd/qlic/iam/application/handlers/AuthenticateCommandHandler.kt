package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.AuthenticateCommand
import com.wasd.qlic.iam.domain.exceptions.AccountAccessDeniedException
import com.wasd.qlic.iam.domain.exceptions.InvalidCredentialsException
import com.wasd.qlic.iam.domain.exceptions.InvalidEmailException
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.AuthenticationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Result of the authentication: subscriber, account, permission level and issued access token. */
data class AuthenticatedSession(
    val subscriber: Subscriber,
    val account: Account,
    val permission: PermissionLevel,
    val accessToken: AccessToken
)

/**
 * US06: invokes AuthenticationService and returns the issued token or rejects the request.
 * US08: when an account shared with the subscriber is requested, the token carries the
 * permission level granted by the invitation.
 */
@Component
class AuthenticateCommandHandler(
    private val subscriberRepository: SubscriberRepository,
    private val accountRepository: AccountRepository,
    private val authorizedUserRepository: AuthorizedUserRepository,
    private val authenticationService: AuthenticationService
) {
    @Transactional(readOnly = true)
    fun handle(command: AuthenticateCommand): AuthenticatedSession {
        val email = try {
            EmailAddress.of(command.email)
        } catch (exception: InvalidEmailException) {
            throw InvalidCredentialsException()
        }

        val subscriber = authenticationService.verifyCredentials(
            subscriberRepository.findByEmail(email), command.password
        )
        val ownAccount = accountRepository.findByOwnerId(subscriber.id)

        if (command.accountId == null || command.accountId == ownAccount?.id) {
            val token = authenticationService.authenticate(subscriber, command.password, ownAccount)
            return AuthenticatedSession(subscriber, ownAccount!!, PermissionLevel.OWNER, token)
        }

        val authorizedUser = authorizedUserRepository.findByAccountIdAndSubscriberId(command.accountId, subscriber.id)
            ?: throw AccountAccessDeniedException()
        val sharedAccount = accountRepository.findById(command.accountId) ?: throw AccountAccessDeniedException()
        val token = authenticationService.authenticate(
            subscriber, command.password, sharedAccount, authorizedUser.permission
        )
        return AuthenticatedSession(subscriber, sharedAccount, authorizedUser.permission, token)
    }
}
