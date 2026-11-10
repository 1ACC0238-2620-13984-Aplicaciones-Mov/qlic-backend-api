package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.AcceptInvitationCommand
import com.wasd.qlic.iam.domain.exceptions.InvitationNotFoundException
import com.wasd.qlic.iam.domain.exceptions.SubscriberNotFoundException
import com.wasd.qlic.iam.domain.exceptions.UserAlreadyInvitedException
import com.wasd.qlic.iam.domain.model.entities.AuthorizedUser
import com.wasd.qlic.iam.domain.model.events.UserAuthorizedEvent
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.InvitationRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US08: the invited person accepts the invitation and becomes an authorized user of the account.
 */
@Component
class AcceptInvitationCommandHandler(
    private val invitationRepository: InvitationRepository,
    private val subscriberRepository: SubscriberRepository,
    private val authorizedUserRepository: AuthorizedUserRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: AcceptInvitationCommand): AuthorizedUser {
        val invitation = invitationRepository.findById(command.invitationId) ?: throw InvitationNotFoundException()
        val subscriber = subscriberRepository.findById(command.subscriberId) ?: throw SubscriberNotFoundException()
        if (authorizedUserRepository.findByAccountIdAndSubscriberId(invitation.accountId, subscriber.id) != null) {
            throw UserAlreadyInvitedException(subscriber.email.value)
        }

        val authorizedUser = invitation.accept(subscriber.id, subscriber.email)
        invitationRepository.save(invitation)
        val saved = authorizedUserRepository.save(authorizedUser)

        eventPublisher.publishEvent(
            UserAuthorizedEvent(
                accountId = saved.accountId,
                subscriberId = saved.subscriberId,
                permission = saved.permission.name
            )
        )
        return saved
    }
}
