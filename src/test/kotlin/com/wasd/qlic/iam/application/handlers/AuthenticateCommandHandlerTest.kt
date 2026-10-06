package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.AuthenticateCommand
import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.domain.exceptions.InvalidCredentialsException
import com.wasd.qlic.iam.domain.services.AuthenticationService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class AuthenticateCommandHandlerTest {

    private lateinit var handler: AuthenticateCommandHandler

    @BeforeEach
    fun setUp() {
        val subscribers = InMemorySubscriberRepository()
        val accounts = InMemoryAccountRepository()
        val hasher = FakePasswordHasher()
        RegisterAccountCommandHandler(subscribers, accounts, hasher, ApplicationEventPublisher { })
            .handle(
                RegisterAccountCommand(
                    fullName = "Carlos Abanto",
                    email = "carlos.abanto@example.com",
                    password = "Qlic2026!",
                    acceptTerms = true
                )
            )
        handler = AuthenticateCommandHandler(
            subscribers, accounts, AuthenticationService(hasher, FakeTokenProvider())
        )
    }

    @Test
    @DisplayName("US06 - Scenario 1: valid credentials start the session and issue a token")
    fun shouldAuthenticateWithValidCredentials() {
        val session = handler.handle(AuthenticateCommand("carlos.abanto@example.com", "Qlic2026!"))

        assertEquals("carlos.abanto@example.com", session.subscriber.email.value)
        assertEquals(session.subscriber.id, session.account.ownerId)
        assertTrue(session.accessToken.value.isNotBlank())
        assertEquals("Bearer", session.accessToken.type)
    }

    @Test
    @DisplayName("US06 - Scenario 2: a wrong password is rejected")
    fun shouldRejectWrongPassword() {
        assertThrows<InvalidCredentialsException> {
            handler.handle(AuthenticateCommand("carlos.abanto@example.com", "wrong-password"))
        }
    }

    @Test
    @DisplayName("US06 - Scenario 2: an unknown email is rejected with the same error")
    fun shouldRejectUnknownEmail() {
        assertThrows<InvalidCredentialsException> {
            handler.handle(AuthenticateCommand("nobody@example.com", "Qlic2026!"))
        }
    }
}
