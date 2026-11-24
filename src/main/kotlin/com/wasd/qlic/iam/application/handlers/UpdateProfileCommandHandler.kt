package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.UpdateProfileCommand
import com.wasd.qlic.iam.domain.exceptions.AccountAccessDeniedException
import com.wasd.qlic.iam.domain.exceptions.SubscriberNotFoundException
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.AuthorizationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US09: validates the mandatory data and persists the changes of the profile and,
 * when the requester is the owner, of the name of the premises.
 */
@Component
class UpdateProfileCommandHandler(
    private val subscriberRepository: SubscriberRepository,
    private val accountRepository: AccountRepository,
    private val authorizationService: AuthorizationService
) {
    @Transactional
    fun handle(command: UpdateProfileCommand): Profile {
        val subscriber = subscriberRepository.findById(command.subscriberId) ?: throw SubscriberNotFoundException()
        val account = accountRepository.findById(command.accountId) ?: throw AccountAccessDeniedException()

        val newAccountName = command.accountName?.takeIf { it.isNotBlank() && it.trim() != account.name }
        if (newAccountName != null) authorizationService.requireOwner(command.requesterPermission)

        subscriber.updateProfile(command.fullName, command.phone)
        if (newAccountName != null) {
            account.rename(newAccountName)
            accountRepository.save(account)
        }
        return Profile(subscriberRepository.save(subscriber), account)
    }
}
