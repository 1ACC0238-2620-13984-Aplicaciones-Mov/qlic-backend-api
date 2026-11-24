package com.wasd.qlic.iam.interfaces.rest

import com.wasd.qlic.iam.application.commands.AcceptInvitationCommand
import com.wasd.qlic.iam.application.commands.InviteUserCommand
import com.wasd.qlic.iam.application.commands.RevokeAuthorizedUserCommand
import com.wasd.qlic.iam.application.handlers.AcceptInvitationCommandHandler
import com.wasd.qlic.iam.application.handlers.AccountQueryHandler
import com.wasd.qlic.iam.application.handlers.InviteUserCommandHandler
import com.wasd.qlic.iam.application.handlers.RevokeAuthorizedUserCommandHandler
import com.wasd.qlic.iam.application.queries.GetAuthorizedUsersQuery
import com.wasd.qlic.iam.application.queries.GetInvitationsByAccountQuery
import com.wasd.qlic.iam.application.queries.GetReceivedInvitationsQuery
import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Invitation
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.interfaces.rest.resources.AuthorizedUserResource
import com.wasd.qlic.iam.interfaces.rest.resources.InvitationResource
import com.wasd.qlic.iam.interfaces.rest.resources.InviteUserResource
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
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Tag(name = "Shared Access", description = "IAM - Share the access to the account with other users (US08)")
class InvitationController(
    private val inviteUserCommandHandler: InviteUserCommandHandler,
    private val acceptInvitationCommandHandler: AcceptInvitationCommandHandler,
    private val revokeAuthorizedUserCommandHandler: RevokeAuthorizedUserCommandHandler,
    private val accountQueryHandler: AccountQueryHandler
) {

    @PostMapping("/api/v1/invitations")
    @Operation(
        summary = "Invite a user to the account (US08 - Scenario 1)",
        description = "Only the owner can invite. The number of additional users is limited by the current plan."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Invitation registered and notified"),
        ApiResponse(
            responseCode = "403", description = "The requester is not the owner of the account",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "Plan limit reached or the person was already invited",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun invite(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: InviteUserResource
    ): ResponseEntity<InvitationResource> {
        val invitation = inviteUserCommandHandler.handle(
            InviteUserCommand(
                accountId = jwt.accountId(),
                requesterId = jwt.subscriberId(),
                requesterPermission = PermissionLevel.valueOf(jwt.permissionName()),
                email = resource.email,
                permission = requireNotNull(resource.permission)
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(invitation.toResource(null))
    }

    @GetMapping("/api/v1/invitations")
    @Operation(summary = "List the invitations issued by the account of the session (US08)")
    fun getIssuedInvitations(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<InvitationResource>> =
        ResponseEntity.ok(
            accountQueryHandler.handle(GetInvitationsByAccountQuery(jwt.accountId())).map { it.toResource(null) }
        )

    @GetMapping("/api/v1/invitations/received")
    @Operation(summary = "List the pending invitations addressed to me (US08)")
    fun getReceivedInvitations(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<InvitationResource>> =
        ResponseEntity.ok(
            accountQueryHandler.handle(GetReceivedInvitationsQuery(jwt.subscriberId()))
                .map { (invitation, account) -> invitation.toResource(account) }
        )

    @PostMapping("/api/v1/invitations/{invitationId}/acceptance")
    @Operation(
        summary = "Accept an invitation (US08)",
        description = "The invited person must be registered with the invited email and sign in first."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Access granted"),
        ApiResponse(
            responseCode = "403", description = "The invitation is addressed to another email",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "The invitation expired or was already used",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun accept(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable invitationId: UUID
    ): ResponseEntity<AuthorizedUserResource> {
        val authorized = acceptInvitationCommandHandler.handle(AcceptInvitationCommand(invitationId, jwt.subscriberId()))
        return ResponseEntity.status(HttpStatus.CREATED).body(
            AuthorizedUserResource(
                id = authorized.id,
                subscriberId = authorized.subscriberId,
                fullName = null,
                email = jwt.getClaimAsString("email"),
                permission = authorized.permission.name,
                authorizedAt = authorized.authorizedAt
            )
        )
    }

    @GetMapping("/api/v1/authorized-users")
    @Operation(summary = "List the authorized users of the account of the session (US08)")
    fun getAuthorizedUsers(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<AuthorizedUserResource>> =
        ResponseEntity.ok(
            accountQueryHandler.handle(GetAuthorizedUsersQuery(jwt.accountId())).map {
                AuthorizedUserResource(
                    id = it.authorizedUser.id,
                    subscriberId = it.authorizedUser.subscriberId,
                    fullName = it.subscriber?.fullName,
                    email = it.subscriber?.email?.value,
                    permission = it.authorizedUser.permission.name,
                    authorizedAt = it.authorizedUser.authorizedAt
                )
            }
        )

    @DeleteMapping("/api/v1/authorized-users/{authorizedUserId}")
    @Operation(summary = "Revoke the access of an authorized user (US08)", description = "Only the owner can revoke access.")
    fun revoke(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable authorizedUserId: UUID
    ): ResponseEntity<Void> {
        revokeAuthorizedUserCommandHandler.handle(
            RevokeAuthorizedUserCommand(
                accountId = jwt.accountId(),
                requesterPermission = PermissionLevel.valueOf(jwt.permissionName()),
                authorizedUserId = authorizedUserId
            )
        )
        return ResponseEntity.noContent().build()
    }

    private fun Invitation.toResource(account: Account?) = InvitationResource(
        id = id,
        accountId = accountId,
        accountName = account?.name,
        invitedEmail = invitedEmail.value,
        permission = permission.name,
        state = if (state.name == "PENDING" && !isPending()) "EXPIRED" else state.name,
        createdAt = createdAt,
        expiresAt = expiresAt
    )
}
