package com.wasd.qlic.iam.interfaces.rest.resources

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Data required to register a subscriber account (US05).")
data class SignUpResource(
    @field:NotBlank(message = "{validation.full-name.required}")
    @field:Size(max = 120, message = "{validation.full-name.size}")
    @field:Schema(example = "Claudia Morales")
    val fullName: String,

    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.format}")
    @field:Schema(example = "claudia.morales@example.com")
    val email: String,

    @field:NotBlank(message = "{validation.password.required}")
    @field:Size(min = 8, max = 72, message = "{validation.password.size}")
    @field:Schema(example = "Qlic2026!", format = "password")
    val password: String,

    @field:Size(max = 20, message = "{validation.phone.size}")
    @field:Schema(example = "+51987654321", nullable = true)
    val phone: String? = null,

    @field:AssertTrue(message = "{validation.terms.required}")
    @field:Schema(example = "true")
    val acceptTerms: Boolean
)

@Schema(description = "Credentials to sign in to the mobile application (US06).")
data class SignInResource(
    @field:NotBlank(message = "{validation.email.required}")
    @field:Schema(example = "claudia.morales@example.com")
    val email: String,

    @field:NotBlank(message = "{validation.password.required}")
    @field:Schema(example = "Qlic2026!", format = "password")
    val password: String
)

@Schema(description = "Created subscriber account.")
data class AccountResource(
    val subscriberId: UUID,
    val accountId: UUID,
    @field:Schema(example = "claudia.morales@example.com") val email: String,
    @field:Schema(example = "Claudia Morales") val fullName: String,
    @field:Schema(example = "+51987654321") val phone: String?,
    @field:Schema(example = "Claudia Morales's account") val accountName: String,
    val createdAt: Instant
)

@Schema(description = "Authenticated session with the issued access token.")
data class AuthenticationResource(
    val subscriberId: UUID,
    val accountId: UUID,
    @field:Schema(example = "claudia.morales@example.com") val email: String,
    @field:Schema(example = "Claudia Morales") val fullName: String,
    @field:Schema(description = "JWT signed with HS256. Send it in the Authorization header.")
    val token: String,
    @field:Schema(example = "Bearer") val tokenType: String,
    val expiresAt: Instant
)
