package com.wasd.qlic.iam.interfaces.rest

import com.wasd.qlic.iam.application.commands.AuthenticateCommand
import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.application.handlers.AuthenticateCommandHandler
import com.wasd.qlic.iam.application.handlers.RegisterAccountCommandHandler
import com.wasd.qlic.iam.interfaces.rest.resources.AccountResource
import com.wasd.qlic.iam.interfaces.rest.resources.AuthenticationResource
import com.wasd.qlic.iam.interfaces.rest.resources.SignInResource
import com.wasd.qlic.iam.interfaces.rest.resources.SignUpResource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/authentication")
@Tag(name = "Authentication", description = "IAM - Subscriber sign-up and sign-in (US05, US06, TS01)")
class AuthenticationController(
    private val registerAccountCommandHandler: RegisterAccountCommandHandler,
    private val authenticateCommandHandler: AuthenticateCommandHandler
) {

    @PostMapping("/sign-up")
    @SecurityRequirements // public endpoint: no token required
    @Operation(
        summary = "Register a subscriber account (US05)",
        description = "Creates the account holder and the account. Accepting the terms of service is required."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Account created"),
        ApiResponse(
            responseCode = "400", description = "Invalid data or terms of service not accepted",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "The email is already registered",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun signUp(@Valid @RequestBody resource: SignUpResource): ResponseEntity<AccountResource> {
        val result = registerAccountCommandHandler.handle(
            RegisterAccountCommand(
                fullName = resource.fullName,
                email = resource.email,
                password = resource.password,
                phone = resource.phone,
                acceptTerms = resource.acceptTerms
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AccountResource(
                subscriberId = result.subscriber.id,
                accountId = result.account.id,
                email = result.subscriber.email.value,
                fullName = result.subscriber.fullName,
                phone = result.subscriber.phone,
                accountName = result.account.name,
                createdAt = result.account.createdAt
            )
        )
    }

    @PostMapping("/sign-in")
    @SecurityRequirements // public endpoint: no token required
    @Operation(
        summary = "Sign in to the mobile application (US06)",
        description = "Validates the credentials and issues a JWT access token with its expiration time (TS01)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Authenticated; returns the access token"),
        ApiResponse(
            responseCode = "400", description = "Incomplete request",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "401", description = "Invalid credentials",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun signIn(@Valid @RequestBody resource: SignInResource): ResponseEntity<AuthenticationResource> {
        val session = authenticateCommandHandler.handle(
            AuthenticateCommand(email = resource.email, password = resource.password)
        )
        return ResponseEntity.ok(
            AuthenticationResource(
                subscriberId = session.subscriber.id,
                accountId = session.account.id,
                email = session.subscriber.email.value,
                fullName = session.subscriber.fullName,
                token = session.accessToken.value,
                tokenType = session.accessToken.type,
                expiresAt = session.accessToken.expiresAt
            )
        )
    }
}
