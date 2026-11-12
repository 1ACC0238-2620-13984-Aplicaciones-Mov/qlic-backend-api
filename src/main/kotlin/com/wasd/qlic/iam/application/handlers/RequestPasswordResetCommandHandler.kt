package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.RequestPasswordResetCommand
import com.wasd.qlic.iam.domain.exceptions.InvalidEmailException
import com.wasd.qlic.iam.domain.model.entities.PasswordResetToken
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.repositories.PasswordResetTokenRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.IamNotificationSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/** Result of the request; the plain token is only returned when issued (used by the dev profile). */
data class PasswordResetRequest(val issuedToken: String?, val expiresAt: Instant?)

/**
 * US07 - Scenario 1: issues a single-use token of limited validity and sends the reset link.
 * The response is the same whether or not the email is registered, so accounts cannot be enumerated.
 */
@Component
class RequestPasswordResetCommandHandler(
    private val subscriberRepository: SubscriberRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val notificationSender: IamNotificationSender,
    @Value("\${qlic.iam.password-reset.expiration-minutes:30}") private val expirationMinutes: Long,
    @Value("\${qlic.iam.password-reset.link-base:qlic://reset-password?token=}") private val linkBase: String
) {
    @Transactional
    fun handle(command: RequestPasswordResetCommand): PasswordResetRequest {
        val email = try {
            EmailAddress.of(command.email)
        } catch (exception: InvalidEmailException) {
            return PasswordResetRequest(null, null)
        }
        val subscriber = subscriberRepository.findByEmail(email) ?: return PasswordResetRequest(null, null)

        val plainToken = PasswordResetToken.generatePlainToken()
        val expiresAt = Instant.now().plus(Duration.ofMinutes(expirationMinutes))
        passwordResetTokenRepository.save(
            PasswordResetToken(
                subscriberId = subscriber.id,
                tokenHash = PasswordResetToken.hash(plainToken),
                expiresAt = expiresAt
            )
        )
        notificationSender.sendPasswordResetLink(email, linkBase + plainToken, expiresAt)
        return PasswordResetRequest(plainToken, expiresAt)
    }
}
