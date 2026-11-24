package com.wasd.qlic.iam.interfaces.rest

import com.wasd.qlic.iam.application.commands.ConfirmPasswordResetCommand
import com.wasd.qlic.iam.application.commands.RequestPasswordResetCommand
import com.wasd.qlic.iam.application.handlers.ConfirmPasswordResetCommandHandler
import com.wasd.qlic.iam.application.handlers.RequestPasswordResetCommandHandler
import com.wasd.qlic.iam.interfaces.rest.resources.PasswordResetConfirmationResource
import com.wasd.qlic.iam.interfaces.rest.resources.PasswordResetRequestResource
import com.wasd.qlic.iam.interfaces.rest.resources.PasswordResetRequestedResource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/authentication/password-reset")
@Tag(name = "Password Reset", description = "IAM - Recover the access to the account (US07)")
class PasswordResetController(
    private val requestPasswordResetCommandHandler: RequestPasswordResetCommandHandler,
    private val confirmPasswordResetCommandHandler: ConfirmPasswordResetCommandHandler,
    private val messageSource: MessageSource,
    @Value("\${qlic.iam.password-reset.expose-token:false}") private val exposeToken: Boolean
) {

    @PostMapping("/request")
    @SecurityRequirements
    @Operation(
        summary = "Request a password reset link (US07 - Scenario 1)",
        description = "Issues a single-use token valid for a limited time and sends the reset link to the email. " +
            "The answer is the same for unregistered emails."
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Request accepted"),
        ApiResponse(
            responseCode = "400", description = "Incomplete request",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun requestReset(@Valid @RequestBody resource: PasswordResetRequestResource): ResponseEntity<PasswordResetRequestedResource> {
        val result = requestPasswordResetCommandHandler.handle(RequestPasswordResetCommand(resource.email))
        val message = messageSource.getMessage(
            "iam.reset.requested", null, "iam.reset.requested", LocaleContextHolder.getLocale()
        ) ?: "iam.reset.requested"
        return ResponseEntity.accepted().body(
            PasswordResetRequestedResource(
                message = message,
                resetToken = result.issuedToken.takeIf { exposeToken },
                expiresAt = result.expiresAt.takeIf { exposeToken }
            )
        )
    }

    @PostMapping("/confirm")
    @SecurityRequirements
    @Operation(
        summary = "Set a new password with the reset token (US07 - Scenario 2)",
        description = "Validates that the token is valid and unused, updates the password and invalidates the token."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Password updated"),
        ApiResponse(
            responseCode = "400", description = "Expired, used or unknown token; a new link must be requested",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun confirmReset(@Valid @RequestBody resource: PasswordResetConfirmationResource): ResponseEntity<Void> {
        confirmPasswordResetCommandHandler.handle(ConfirmPasswordResetCommand(resource.token, resource.newPassword))
        return ResponseEntity.noContent().build()
    }
}
