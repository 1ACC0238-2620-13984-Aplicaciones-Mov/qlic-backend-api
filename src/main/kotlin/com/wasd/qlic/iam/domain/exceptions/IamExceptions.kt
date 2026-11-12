package com.wasd.qlic.iam.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.DomainException
import com.wasd.qlic.shared.domain.exceptions.OperationNotPermittedException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** US05 - Scenario 2: the email already belongs to an existing account. */
class EmailAlreadyRegisteredException(email: String) :
    DomainException("iam.email.already-registered", email)

/** US05: registration requires accepting the terms of service. */
class TermsNotAcceptedException : DomainException("iam.terms.not-accepted")

/** US06 - Scenario 2: wrong credentials (same message for unknown email and wrong password). */
class InvalidCredentialsException : DomainException("iam.credentials.invalid")

/** The email address does not have a valid format. */
class InvalidEmailException : DomainException("iam.email.invalid")

/** US07 - Scenario 2: the reset link expired; a new one must be requested. */
class ResetTokenExpiredException : DomainException("iam.reset-token.expired")

/** US07: the reset token does not exist or was already used. */
class ResetTokenInvalidException : DomainException("iam.reset-token.invalid")

/** US08 - Scenario 2: the permission level does not allow the operation. */
class PermissionDeniedException : OperationNotPermittedException("iam.permission.denied")

/** US08: the subscriber is not an authorized user of the requested account. */
class AccountAccessDeniedException : OperationNotPermittedException("iam.account.access-denied")

/** US08 - Business rule: the plan limits the number of additional users. */
class AuthorizedUsersLimitReachedException(limit: Int) :
    BusinessConflictException("iam.invitation.limit-reached", limit)

/** US08: the person already has access to the account or has a pending invitation. */
class UserAlreadyInvitedException(email: String) :
    BusinessConflictException("iam.invitation.already-invited", email)

/** US08: the owner cannot invite themselves and cannot delegate the OWNER level. */
class InvalidInvitationException : DomainException("iam.invitation.invalid")

class InvitationNotFoundException : ResourceNotFoundException("iam.invitation.not-found")

class InvitationNotAddressedToSubscriberException : OperationNotPermittedException("iam.invitation.not-addressed")

class InvitationNotPendingException : BusinessConflictException("iam.invitation.not-pending")

class InvitationExpiredException : BusinessConflictException("iam.invitation.expired")

class AuthorizedUserNotFoundException : ResourceNotFoundException("iam.authorized-user.not-found")

class SubscriberNotFoundException : ResourceNotFoundException("iam.subscriber.not-found")
