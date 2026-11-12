package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.domain.exceptions.EmailAlreadyRegisteredException
import com.wasd.qlic.iam.domain.exceptions.TermsNotAcceptedException
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.events.AccountCreatedEvent
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import com.wasd.qlic.iam.domain.services.PasswordHasher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/** Result of the registration: the account holder and the created account. */
data class RegisteredAccount(val subscriber: Subscriber, val account: Account)

/**
 * US05: validates the email uniqueness, records the acceptance of the terms of service
 * and creates the subscriber account.
 */
@Component
class RegisterAccountCommandHandler(
    private val subscriberRepository: SubscriberRepository,
    private val accountRepository: AccountRepository,
    private val passwordHasher: PasswordHasher,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: RegisterAccountCommand): RegisteredAccount {
        if (!command.acceptTerms) throw TermsNotAcceptedException()

        val email = EmailAddress.of(command.email)
        if (subscriberRepository.existsByEmail(email)) {
            throw EmailAlreadyRegisteredException(email.value)
        }

        val subscriber = subscriberRepository.save(
            Subscriber(
                email = email,
                passwordHash = passwordHasher.hash(command.password),
                fullName = command.fullName.trim(),
                phone = command.phone?.trim()?.takeIf { it.isNotEmpty() },
                termsAcceptedAt = Instant.now()
            )
        )
        val account = accountRepository.save(Account.openFor(subscriber))

        eventPublisher.publishEvent(
            AccountCreatedEvent(accountId = account.id, subscriberId = subscriber.id, email = email.value)
        )
        return RegisteredAccount(subscriber, account)
    }
}
