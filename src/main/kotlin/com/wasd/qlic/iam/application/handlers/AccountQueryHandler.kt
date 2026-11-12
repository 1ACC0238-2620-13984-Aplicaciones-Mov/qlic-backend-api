package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.queries.GetAuthorizedUsersQuery
import com.wasd.qlic.iam.application.queries.GetInvitationsByAccountQuery
import com.wasd.qlic.iam.application.queries.GetProfileQuery
import com.wasd.qlic.iam.application.queries.GetReceivedInvitationsQuery
import com.wasd.qlic.iam.application.queries.GetSharedAccountsQuery
import com.wasd.qlic.iam.domain.exceptions.AccountAccessDeniedException
import com.wasd.qlic.iam.domain.exceptions.SubscriberNotFoundException
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.AuthorizedUser
import com.wasd.qlic.iam.domain.model.entities.Invitation
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.repositories.InvitationRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Profile of a subscriber together with the account of the session (US09). */
data class Profile(val subscriber: Subscriber, val account: Account)

/** Authorized user with the data of the person (US08). */
data class AuthorizedUserView(val authorizedUser: AuthorizedUser, val subscriber: Subscriber?)

/** Account shared with the requester and the permission granted (US08). */
data class SharedAccountView(val account: Account, val authorizedUser: AuthorizedUser)

/** Read side of IAM: profile, invitations, authorized users and shared accounts. */
@Component
@Transactional(readOnly = true)
class AccountQueryHandler(
    private val subscriberRepository: SubscriberRepository,
    private val accountRepository: AccountRepository,
    private val invitationRepository: InvitationRepository,
    private val authorizedUserRepository: AuthorizedUserRepository
) {
    fun handle(query: GetProfileQuery): Profile {
        val subscriber = subscriberRepository.findById(query.subscriberId) ?: throw SubscriberNotFoundException()
        val account = accountRepository.findById(query.accountId) ?: throw AccountAccessDeniedException()
        return Profile(subscriber, account)
    }

    fun handle(query: GetInvitationsByAccountQuery): List<Invitation> =
        invitationRepository.findByAccountId(query.accountId).sortedByDescending { it.createdAt }

    fun handle(query: GetReceivedInvitationsQuery): List<Pair<Invitation, Account?>> {
        val subscriber = subscriberRepository.findById(query.subscriberId) ?: throw SubscriberNotFoundException()
        return invitationRepository.findByInvitedEmail(subscriber.email)
            .filter { it.isPending() }
            .map { it to accountRepository.findById(it.accountId) }
    }

    fun handle(query: GetAuthorizedUsersQuery): List<AuthorizedUserView> =
        authorizedUserRepository.findByAccountId(query.accountId)
            .map { AuthorizedUserView(it, subscriberRepository.findById(it.subscriberId)) }

    fun handle(query: GetSharedAccountsQuery): List<SharedAccountView> =
        authorizedUserRepository.findBySubscriberId(query.subscriberId)
            .mapNotNull { authorized -> accountRepository.findById(authorized.accountId)?.let { SharedAccountView(it, authorized) } }
}
