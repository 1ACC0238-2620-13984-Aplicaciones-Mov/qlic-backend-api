package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.domain.exceptions.EmailAlreadyRegisteredException
import com.wasd.qlic.iam.domain.exceptions.InvalidEmailException
import com.wasd.qlic.iam.domain.exceptions.TermsNotAcceptedException
import com.wasd.qlic.iam.domain.model.events.AccountCreatedEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class RegisterAccountCommandHandlerTest {

    private lateinit var subscribers: InMemorySubscriberRepository
    private lateinit var accounts: InMemoryAccountRepository
    private val publishedEvents = mutableListOf<Any>()
    private lateinit var handler: RegisterAccountCommandHandler

    @BeforeEach
    fun setUp() {
        subscribers = InMemorySubscriberRepository()
        accounts = InMemoryAccountRepository()
        publishedEvents.clear()
        handler = RegisterAccountCommandHandler(
            subscribers, accounts, FakePasswordHasher(),
            ApplicationEventPublisher { publishedEvents.add(it) }
        )
    }

    private fun command(email: String = "claudia.morales@example.com", acceptTerms: Boolean = true) =
        RegisterAccountCommand(
            fullName = "Claudia Morales",
            email = email,
            password = "Qlic2026!",
            phone = "+51987654321",
            acceptTerms = acceptTerms
        )

    @Test
    @DisplayName("US05 - Scenario 1: valid sign-up creates subscriber and account and publishes AccountCreatedEvent")
    fun shouldRegisterAccountWhenDataIsValid() {
        val result = handler.handle(command())

        assertEquals("claudia.morales@example.com", result.subscriber.email.value)
        assertEquals(result.subscriber.id, result.account.ownerId)
        assertNotEquals("Qlic2026!", result.subscriber.passwordHash.value)
        assertEquals(1, subscribers.count())
        assertEquals(1, accounts.count())
        assertTrue(publishedEvents.single() is AccountCreatedEvent)
    }

    @Test
    @DisplayName("US05 - Scenario 2: an already registered email does not create a new account")
    fun shouldRejectDuplicatedEmail() {
        handler.handle(command())

        assertThrows<EmailAlreadyRegisteredException> {
            handler.handle(command(email = "  Claudia.Morales@Example.com "))
        }
        assertEquals(1, subscribers.count())
        assertEquals(1, accounts.count())
    }

    @Test
    @DisplayName("US05 - Rule: the account is not created without accepting the terms of service")
    fun shouldRejectWhenTermsAreNotAccepted() {
        assertThrows<TermsNotAcceptedException> { handler.handle(command(acceptTerms = false)) }
        assertEquals(0, subscribers.count())
    }

    @Test
    @DisplayName("US05 - Rule: an email with invalid format is rejected")
    fun shouldRejectInvalidEmail() {
        assertThrows<InvalidEmailException> { handler.handle(command(email = "not-an-email")) }
        assertEquals(0, subscribers.count())
    }
}
