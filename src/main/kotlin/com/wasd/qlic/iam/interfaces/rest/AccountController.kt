package com.wasd.qlic.iam.interfaces.rest

import com.wasd.qlic.iam.application.commands.UpdateProfileCommand
import com.wasd.qlic.iam.application.handlers.AccountQueryHandler
import com.wasd.qlic.iam.application.handlers.Profile
import com.wasd.qlic.iam.application.handlers.UpdateProfileCommandHandler
import com.wasd.qlic.iam.application.queries.GetProfileQuery
import com.wasd.qlic.iam.application.queries.GetSharedAccountsQuery
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.interfaces.rest.resources.ProfileResource
import com.wasd.qlic.iam.interfaces.rest.resources.SharedAccountResource
import com.wasd.qlic.iam.interfaces.rest.resources.UpdateProfileResource
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import com.wasd.qlic.shared.interfaces.rest.security.permissionName
import com.wasd.qlic.shared.interfaces.rest.security.subscriberId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "IAM - Profile of the subscriber and accounts shared with them (US08, US09)")
class AccountController(
    private val accountQueryHandler: AccountQueryHandler,
    private val updateProfileCommandHandler: UpdateProfileCommandHandler
) {

    @GetMapping("/me")
    @Operation(summary = "Get the profile and the account of the session (US09)")
    fun getProfile(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<ProfileResource> {
        val profile = accountQueryHandler.handle(GetProfileQuery(jwt.subscriberId(), jwt.accountId()))
        return ResponseEntity.ok(profile.toResource(jwt.permissionName()))
    }

    @PutMapping("/me")
    @Operation(
        summary = "Update the profile data (US09)",
        description = "Updates the full name and phone. The name of the premises (accountName) can only be changed by the owner."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Profile updated"),
        ApiResponse(
            responseCode = "400", description = "A mandatory field is empty; the errors object indicates which one",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "403", description = "Only the owner can rename the premises",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun updateProfile(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: UpdateProfileResource
    ): ResponseEntity<ProfileResource> {
        val profile = updateProfileCommandHandler.handle(
            UpdateProfileCommand(
                subscriberId = jwt.subscriberId(),
                accountId = jwt.accountId(),
                requesterPermission = PermissionLevel.valueOf(jwt.permissionName()),
                fullName = resource.fullName,
                phone = resource.phone,
                accountName = resource.accountName
            )
        )
        return ResponseEntity.ok(profile.toResource(jwt.permissionName()))
    }

    @GetMapping("/shared-with-me")
    @Operation(
        summary = "List the accounts shared with me (US08)",
        description = "Use the accountId of an item in POST /api/v1/authentication/sign-in to open a session over it."
    )
    fun getSharedAccounts(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<SharedAccountResource>> =
        ResponseEntity.ok(
            accountQueryHandler.handle(GetSharedAccountsQuery(jwt.subscriberId())).map {
                SharedAccountResource(
                    accountId = it.account.id,
                    accountName = it.account.name,
                    permission = it.authorizedUser.permission.name,
                    authorizedAt = it.authorizedUser.authorizedAt
                )
            }
        )

    private fun Profile.toResource(permission: String) = ProfileResource(
        subscriberId = subscriber.id,
        accountId = account.id,
        email = subscriber.email.value,
        fullName = subscriber.fullName,
        phone = subscriber.phone,
        accountName = account.name,
        permission = permission,
        memberSince = subscriber.createdAt
    )
}
