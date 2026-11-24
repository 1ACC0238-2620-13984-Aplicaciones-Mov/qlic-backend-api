package com.wasd.qlic.iam.interfaces.rest.resources

import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Email of the account whose password must be reset (US07 - Scenario 1).")
data class PasswordResetRequestResource(
    @field:NotBlank(message = "{validation.email.required}")
    @field:Schema(example = "claudia.morales@example.com")
    val email: String
)

@Schema(description = "Answer to a password reset request. The message is the same whether or not the email exists.")
data class PasswordResetRequestedResource(
    val message: String,
    @field:Schema(
        description = "Only returned when qlic.iam.password-reset.expose-token is enabled (dev profile), " +
            "because no email provider is configured yet.",
        nullable = true
    )
    val resetToken: String? = null,
    val expiresAt: Instant? = null
)

@Schema(description = "Token of the reset link and the new password (US07 - Scenario 2).")
data class PasswordResetConfirmationResource(
    @field:NotBlank(message = "{validation.reset-token.required}")
    val token: String,

    @field:NotBlank(message = "{validation.password.required}")
    @field:Size(min = 8, max = 72, message = "{validation.password.size}")
    @field:Schema(example = "NuevaClave2026!", format = "password")
    val newPassword: String
)

@Schema(description = "Profile of the subscriber and data of the account of the session (US09).")
data class ProfileResource(
    val subscriberId: UUID,
    val accountId: UUID,
    @field:Schema(example = "claudia.morales@example.com") val email: String,
    @field:Schema(example = "Claudia Morales") val fullName: String,
    @field:Schema(example = "+51987654321") val phone: String?,
    @field:Schema(description = "Name of the household or business premises", example = "Bodega Morales")
    val accountName: String,
    @field:Schema(example = "OWNER") val permission: String,
    val memberSince: Instant
)

@Schema(description = "Profile data to update (US09). Only the owner can rename the premises.")
data class UpdateProfileResource(
    @field:NotBlank(message = "{validation.full-name.required}")
    @field:Size(max = 120, message = "{validation.full-name.size}")
    @field:Schema(example = "Claudia Morales Ríos")
    val fullName: String,

    @field:Size(max = 20, message = "{validation.phone.size}")
    @field:Schema(example = "+51912345678", nullable = true)
    val phone: String? = null,

    @field:Size(max = 120, message = "{validation.account-name.size}")
    @field:Schema(example = "Bodega Morales - Surco", nullable = true)
    val accountName: String? = null
)

@Schema(description = "Person to invite and permission level (US08 - Scenario 1).")
data class InviteUserResource(
    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.format}")
    @field:Schema(example = "jorge.quispe@example.com")
    val email: String,

    @field:NotNull(message = "{validation.permission.required}")
    @field:Schema(description = "VIEWER (read only) or OPERATOR (read and modify)", example = "VIEWER")
    val permission: PermissionLevel?
)

@Schema(description = "Access invitation (US08).")
data class InvitationResource(
    val id: UUID,
    val accountId: UUID,
    @field:Schema(example = "Bodega Morales") val accountName: String?,
    @field:Schema(example = "jorge.quispe@example.com") val invitedEmail: String,
    @field:Schema(example = "VIEWER") val permission: String,
    @field:Schema(example = "PENDING") val state: String,
    val createdAt: Instant,
    val expiresAt: Instant
)

@Schema(description = "User with delegated access to the account (US08).")
data class AuthorizedUserResource(
    val id: UUID,
    val subscriberId: UUID,
    @field:Schema(example = "Jorge Quispe") val fullName: String?,
    @field:Schema(example = "jorge.quispe@example.com") val email: String?,
    @field:Schema(example = "VIEWER") val permission: String,
    val authorizedAt: Instant
)

@Schema(description = "Account shared with the subscriber. Use its accountId to sign in to it (US08).")
data class SharedAccountResource(
    val accountId: UUID,
    @field:Schema(example = "Bodega Morales") val accountName: String,
    @field:Schema(example = "VIEWER") val permission: String,
    val authorizedAt: Instant
)
