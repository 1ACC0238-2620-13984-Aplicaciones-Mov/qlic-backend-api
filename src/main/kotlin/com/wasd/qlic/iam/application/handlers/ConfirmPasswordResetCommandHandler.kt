package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.ConfirmPasswordResetCommand
import com.wasd.qlic.iam.domain.exceptions.ResetTokenInvalidException
import com.wasd.qlic.iam.domain.exceptions.SubscriberNotFoundException
import com.wasd.qlic.iam.domain.model.entities.PasswordResetToken
import com.wasd.qlic.iam.domain.repositories.PasswordResetTokenRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.PasswordHasher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * US07 - Scenario 2: validates the validity of the token, updates the password and invalidates the token.
 */
@Component
class ConfirmPasswordResetCommandHandler(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val subscriberRepository: SubscriberRepository,
    private val passwordHasher: PasswordHasher
) {
    @Transactional
    fun handle(command: ConfirmPasswordResetCommand) {
        val resetToken = passwordResetTokenRepository.findByTokenHash(PasswordResetToken.hash(command.token))
            ?: throw ResetTokenInvalidException()
        resetToken.consume()

        val subscriber = subscriberRepository.findById(resetToken.subscriberId)
            ?: throw SubscriberNotFoundException()
        subscriber.changePassword(passwordHasher.hash(command.newPassword))
        subscriberRepository.save(subscriber)
        passwordResetTokenRepository.save(resetToken)
    }
}
