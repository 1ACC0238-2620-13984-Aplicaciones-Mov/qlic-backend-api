package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.InviteUserCommand
import com.wasd.qlic.iam.domain.exceptions.AccountAccessDeniedException
import com.wasd.qlic.iam.domain.exceptions.InvalidInvitationException
import com.wasd.qlic.iam.domain.exceptions.UserAlreadyInvitedException
import com.wasd.qlic.iam.domain.model.entities.Invitation
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.InvitationRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.AuthorizationService
import com.wasd.qlic.iam.domain.services.IamNotificationSender
import com.wasd.qlic.iam.domain.services.InvitationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US08 - Scenario 1: checks the plan limit, registers the invitation and notifies the invited person.
 */
@Component
class InviteUserCommandHandler(
    private val accountRepository: AccountRepository,
    private val subscriberRepository: SubscriberRepository,
    private val invitationRepository: InvitationRepository,
    private val authorizedUserRepository: AuthorizedUserRepository,
    private val authorizationService: AuthorizationService,
    private val invitationService: InvitationService,
    private val notificationSender: IamNotificationSender
) {
    @Transactional
    fun handle(command: InviteUserCommand): Invitation {
        authorizationService.requireOwner(command.requesterPermission)
        if (command.permission == PermissionLevel.OWNER) throw InvalidInvitationException()

        val account = accountRepository.findById(command.accountId) ?: throw AccountAccessDeniedException()
        val owner = subscriberRepository.findById(account.ownerId) ?: throw AccountAccessDeniedException()
        val email = EmailAddress.of(command.email)
        if (email == owner.email) throw InvalidInvitationException()

        val alreadyPending = invitationRepository.findByAccountId(account.id)
            .any { it.invitedEmail == email && it.isPending() }
        val invitedSubscriber = subscriberRepository.findByEmail(email)
        val alreadyAuthorized = invitedSubscriber != null &&
            authorizedUserRepository.findByAccountIdAndSubscriberId(account.id, invitedSubscriber.id) != null
        if (alreadyPending || alreadyAuthorized) throw UserAlreadyInvitedException(email.value)

        invitationService.ensureCapacity(account.id)

        val invitation = invitationRepository.save(
            Invitation(
                accountId = account.id,
                invitedEmail = email,
                permission = command.permission,
                invitedBy = command.requesterId
            )
        )
        notificationSender.sendInvitation(email, account.name, invitation.permission.name, invitation.expiresAt)
        return invitation
    }
}
