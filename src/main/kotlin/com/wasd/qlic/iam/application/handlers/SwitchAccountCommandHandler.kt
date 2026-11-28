package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.SwitchAccountCommand
import com.wasd.qlic.iam.domain.exceptions.AccountAccessDeniedException
import com.wasd.qlic.iam.domain.exceptions.SubscriberNotFoundException
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.TokenProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US08: a signed-in subscriber opens a session over another account they can access without typing the password
 * again. The own account gives OWNER; an account shared with the subscriber gives the permission of the invitation.
 * The member application uses it right after sign-in to enter the shared household or business.
 */
@Component
class SwitchAccountCommandHandler(
    private val subscriberRepository: SubscriberRepository,
    private val accountRepository: AccountRepository,
    private val authorizedUserRepository: AuthorizedUserRepository,
    private val tokenProvider: TokenProvider
) {
    @Transactional(readOnly = true)
    fun handle(command: SwitchAccountCommand): AuthenticatedSession {
        val subscriber = subscriberRepository.findById(command.subscriberId) ?: throw SubscriberNotFoundException()
        val account = accountRepository.findById(command.accountId) ?: throw AccountAccessDeniedException()
        val permission = if (account.ownerId == subscriber.id) {
            PermissionLevel.OWNER
        } else {
            authorizedUserRepository.findByAccountIdAndSubscriberId(account.id, subscriber.id)?.permission
                ?: throw AccountAccessDeniedException()
        }
        return AuthenticatedSession(subscriber, account, permission, tokenProvider.issue(subscriber, account, permission))
    }
}
