package com.wasd.qlic.iam.application.commands

import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import java.util.UUID

/** US05: request to create a subscriber account. */
data class RegisterAccountCommand(
    val fullName: String,
    val email: String,
    val password: String,
    val phone: String? = null,
    val acceptTerms: Boolean
)

/**
 * US06: request to authenticate a subscriber. When accountId is sent, the session is opened
 * over an account shared with the subscriber (US08); otherwise over the subscriber's own account.
 */
data class AuthenticateCommand(
    val email: String,
    val password: String,
    val accountId: UUID? = null
)

/** US07 - Scenario 1: request a password reset link. */
data class RequestPasswordResetCommand(val email: String)

/** US07 - Scenario 2: set a new password with the token of the reset link. */
data class ConfirmPasswordResetCommand(val token: String, val newPassword: String)

/** US08 - Scenario 1: invite a person to access the account with a permission level. */
data class InviteUserCommand(
    val accountId: UUID,
    val requesterId: UUID,
    val requesterPermission: PermissionLevel,
    val email: String,
    val permission: PermissionLevel
)

/** US08: the invited person accepts the invitation with their own session. */
data class AcceptInvitationCommand(val invitationId: UUID, val subscriberId: UUID)

/** US08: the owner revokes the access of an authorized user. */
data class RevokeAuthorizedUserCommand(
    val accountId: UUID,
    val requesterPermission: PermissionLevel,
    val authorizedUserId: UUID
)

/** US09: update the profile data and, for the owner, the name of the premises. */
data class UpdateProfileCommand(
    val subscriberId: UUID,
    val accountId: UUID,
    val requesterPermission: PermissionLevel,
    val fullName: String,
    val phone: String?,
    val accountName: String?
)
