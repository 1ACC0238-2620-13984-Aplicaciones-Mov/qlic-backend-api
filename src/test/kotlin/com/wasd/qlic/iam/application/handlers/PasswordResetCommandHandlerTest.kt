package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.AuthenticateCommand
import com.wasd.qlic.iam.application.commands.ConfirmPasswordResetCommand
import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.application.commands.RequestPasswordResetCommand
import com.wasd.qlic.iam.domain.exceptions.ResetTokenExpiredException
import com.wasd.qlic.iam.domain.exceptions.ResetTokenInvalidException
import com.wasd.qlic.iam.domain.model.entities.PasswordResetToken
import com.wasd.qlic.iam.domain.services.AuthenticationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant

class PasswordResetCommandHandlerTest {

    private val subscribers = InMemorySubscriberRepository()
    private val accounts = InMemoryAccountRepository()
    private val tokens = InMemoryPasswordResetTokenRepository()
    private val hasher = FakePasswordHasher()
    private val notifications = RecordingNotificationSender()

    private lateinit var requestHandler: RequestPasswordResetCommandHandler
    private lateinit var confirmHandler: ConfirmPasswordResetCommandHandler
    private lateinit var authenticateHandler: AuthenticateCommandHandler

    @BeforeEach
    fun setUp() {
        RegisterAccountCommandHandler(subscribers, accounts, hasher, ApplicationEventPublisher { })
            .handle(RegisterAccountCommand("Rosa Huamán", "rosa.huaman@example.com", "Qlic2026!", acceptTerms = true))
        requestHandler = RequestPasswordResetCommandHandler(subscribers, tokens, notifications, 30L, "qlic://reset-password?token=")
        confirmHandler = ConfirmPasswordResetCommandHandler(tokens, subscribers, hasher)
        authenticateHandler = AuthenticateCommandHandler(
            subscribers, accounts, InMemoryAuthorizedUserRepository(), AuthenticationService(hasher, FakeTokenProvider())
        )
    }

    @Test
    @DisplayName("US07 - Scenario 1: a registered email receives a reset link of limited validity")
    fun shouldSendResetLinkToRegisteredEmail() {
        val result = requestHandler.handle(RequestPasswordResetCommand("rosa.huaman@example.com"))

        assertNotNull(result.issuedToken)
        assertTrue(result.expiresAt!!.isAfter(Instant.now()))
        assertEquals(1, notifications.resetLinks.size)
        assertTrue(notifications.resetLinks.first().second.endsWith(result.issuedToken!!))
        assertEquals(PasswordResetToken.hash(result.issuedToken!!), tokens.all().single().tokenHash)
    }

    @Test
    @DisplayName("US07 - Scenario 1: an unregistered email gets the same answer and no email is sent")
    fun shouldNotRevealUnregisteredEmails() {
        val result = requestHandler.handle(RequestPasswordResetCommand("nobody@example.com"))

        assertNull(result.issuedToken)
        assertTrue(notifications.resetLinks.isEmpty())
    }

    @Test
    @DisplayName("US07 - Scenario 1: a valid token sets the new password and can only be used once")
    fun shouldResetPasswordOnce() {
        val token = requestHandler.handle(RequestPasswordResetCommand("rosa.huaman@example.com")).issuedToken!!

        confirmHandler.handle(ConfirmPasswordResetCommand(token, "NuevaClave2026!"))

        val session = authenticateHandler.handle(AuthenticateCommand("rosa.huaman@example.com", "NuevaClave2026!"))
        assertEquals("rosa.huaman@example.com", session.subscriber.email.value)
        assertThrows<ResetTokenInvalidException> {
            confirmHandler.handle(ConfirmPasswordResetCommand(token, "OtraClave2026!"))
        }
    }

    @Test
    @DisplayName("US07 - Scenario 2: an expired link is rejected and a new one must be requested")
    fun shouldRejectExpiredToken() {
        val subscriber = subscribers.findByEmail(com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress.of("rosa.huaman@example.com"))!!
        val plain = PasswordResetToken.generatePlainToken()
        tokens.save(
            PasswordResetToken(
                subscriberId = subscriber.id,
                tokenHash = PasswordResetToken.hash(plain),
                expiresAt = Instant.now().minusSeconds(60)
            )
        )

        assertThrows<ResetTokenExpiredException> {
            confirmHandler.handle(ConfirmPasswordResetCommand(plain, "NuevaClave2026!"))
        }
    }
}
