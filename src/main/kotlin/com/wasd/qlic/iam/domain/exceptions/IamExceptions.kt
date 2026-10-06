package com.wasd.qlic.iam.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.DomainException

/** US05 - Scenario 2: the email already belongs to an existing account. */
class EmailAlreadyRegisteredException(email: String) :
    DomainException("iam.email.already-registered", email)

/** US05: registration requires accepting the terms of service. */
class TermsNotAcceptedException : DomainException("iam.terms.not-accepted")

/** US06 - Scenario 2: wrong credentials (same message for unknown email and wrong password). */
class InvalidCredentialsException : DomainException("iam.credentials.invalid")

/** The email address does not have a valid format. */
class InvalidEmailException : DomainException("iam.email.invalid")
