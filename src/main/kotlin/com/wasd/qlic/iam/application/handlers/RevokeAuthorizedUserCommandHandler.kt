package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.RevokeAuthorizedUserCommand
import com.wasd.qlic.iam.domain.exceptions.AuthorizedUserNotFoundException
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import com.wasd.qlic.iam.domain.services.AuthorizationService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** US08: the owner removes the access of an authorized user, freeing a place of the plan. */
@Component
class RevokeAuthorizedUserCommandHandler(
    private val authorizedUserRepository: AuthorizedUserRepository,
    private val authorizationService: AuthorizationService
) {
    @Transactional
    fun handle(command: RevokeAuthorizedUserCommand) {
        authorizationService.requireOwner(command.requesterPermission)
        val authorizedUser = authorizedUserRepository.findById(command.authorizedUserId)
            ?.takeIf { it.accountId == command.accountId }
            ?: throw AuthorizedUserNotFoundException()
        authorizedUserRepository.delete(authorizedUser)
    }
}
