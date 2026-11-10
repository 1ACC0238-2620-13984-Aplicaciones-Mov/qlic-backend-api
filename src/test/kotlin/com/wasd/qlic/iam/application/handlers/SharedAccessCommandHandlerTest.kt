package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.AcceptInvitationCommand
import com.wasd.qlic.iam.application.commands.AuthenticateCommand
import com.wasd.qlic.iam.application.commands.InviteUserCommand
import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.application.commands.RevokeAuthorizedUserCommand
import com.wasd.qlic.iam.domain.exceptions.AccountAccessDeniedException
import com.wasd.qlic.iam.domain.exceptions.AuthorizedUsersLimitReachedException
import com.wasd.qlic.iam.domain.exceptions.InvitationNotAddressedToSubscriberException
import com.wasd.qlic.iam.domain.exceptions.PermissionDeniedException
import com.wasd.qlic.iam.domain.exceptions.UserAlreadyInvitedException
import com.wasd.qlic.iam.domain.model.valueobjects.InvitationState
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.domain.services.AuthenticationService
import com.wasd.qlic.iam.domain.services.AuthorizationService
import com.wasd.qlic.iam.domain.services.InvitationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class SharedAccessCommandHandlerTest {

    private val subscribers = InMemorySubscriberRepository()
    private val accounts = InMemoryAccountRepository()
    private val invitations = InMemoryInvitationRepository()
    private val authorizedUsers = InMemoryAuthorizedUserRepository()
    private val notifications = RecordingNotificationSender()
    private val plan = FixedPlanEntitlementProvider(limit = 2)
    private val hasher = FakePasswordHasher()

    private lateinit var owner: RegisteredAccount
    private lateinit var inviteHandler: InviteUserCommandHandler
    private lateinit var acceptHandler: AcceptInvitationCommandHandler
    private lateinit var authenticateHandler: AuthenticateCommandHandler

    @BeforeEach
    fun setUp() {
        val register = RegisterAccountCommandHandler(subscribers, accounts, hasher, ApplicationEventPublisher { })
        owner = register.handle(RegisterAccountCommand("Claudia Morales", "claudia@example.com", "Qlic2026!", acceptTerms = true))
        register.handle(RegisterAccountCommand("Jorge Quispe", "jorge@example.com", "Qlic2026!", acceptTerms = true))

        inviteHandler = InviteUserCommandHandler(
            accounts, subscribers, invitations, authorizedUsers, AuthorizationService(),
            InvitationService(plan, authorizedUsers, invitations), notifications
        )
        acceptHandler = AcceptInvitationCommandHandler(invitations, subscribers, authorizedUsers, ApplicationEventPublisher { })
        authenticateHandler = AuthenticateCommandHandler(
            subscribers, accounts, authorizedUsers, AuthenticationService(hasher, FakeTokenProvider())
        )
    }

    private fun invite(email: String, permission: PermissionLevel = PermissionLevel.VIEWER, requester: PermissionLevel = PermissionLevel.OWNER) =
        inviteHandler.handle(InviteUserCommand(owner.account.id, owner.subscriber.id, requester, email, permission))

    @Test
    @DisplayName("US08 - Scenario 1: the owner invites a user, the invitation is registered and notified")
    fun shouldRegisterAndNotifyInvitation() {
        val invitation = invite("jorge@example.com")

        assertEquals(InvitationState.PENDING, invitation.state)
        assertEquals(PermissionLevel.VIEWER, invitation.permission)
        assertEquals(listOf("jorge@example.com"), notifications.invitations)
    }

    @Test
    @DisplayName("US08 - Business rule: the plan limits the number of additional users")
    fun shouldRespectPlanLimit() {
        plan.limit = 1
        invite("jorge@example.com")

        assertThrows<AuthorizedUsersLimitReachedException> { invite("maria@example.com") }
    }

    @Test
    @DisplayName("US08: only the owner can invite and the same person cannot be invited twice")
    fun shouldValidateRequesterAndDuplicates() {
        assertThrows<PermissionDeniedException> { invite("jorge@example.com", requester = PermissionLevel.OPERATOR) }
        invite("jorge@example.com")
        assertThrows<UserAlreadyInvitedException> { invite("jorge@example.com") }
    }

    @Test
    @DisplayName("US08 - Scenario 2: the invited user signs in to the shared account with VIEWER permission")
    fun shouldOpenSharedSessionWithGrantedPermission() {
        val invitation = invite("jorge@example.com")
        val jorge = subscribers.findByEmail(com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress.of("jorge@example.com"))!!

        acceptHandler.handle(AcceptInvitationCommand(invitation.id, jorge.id))
        val session = authenticateHandler.handle(AuthenticateCommand("jorge@example.com", "Qlic2026!", owner.account.id))

        assertEquals(owner.account.id, session.account.id)
        assertEquals(PermissionLevel.VIEWER, session.permission)
        assertFalse(session.permission.canModify())
        assertThrows<PermissionDeniedException> { AuthorizationService().requireModification(session.permission) }
    }

    @Test
    @DisplayName("US08: an invitation can only be accepted by the invited email")
    fun shouldRejectAcceptanceByAnotherSubscriber() {
        val invitation = invite("maria@example.com")
        val jorge = subscribers.findByEmail(com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress.of("jorge@example.com"))!!

        assertThrows<InvitationNotAddressedToSubscriberException> {
            acceptHandler.handle(AcceptInvitationCommand(invitation.id, jorge.id))
        }
    }

    @Test
    @DisplayName("US08: after revoking the access, the user cannot sign in to the shared account")
    fun shouldRevokeAccess() {
        val invitation = invite("jorge@example.com", PermissionLevel.OPERATOR)
        val jorge = subscribers.findByEmail(com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress.of("jorge@example.com"))!!
        val authorized = acceptHandler.handle(AcceptInvitationCommand(invitation.id, jorge.id))
        assertTrue(authorized.permission.canModify())

        RevokeAuthorizedUserCommandHandler(authorizedUsers, AuthorizationService())
            .handle(RevokeAuthorizedUserCommand(owner.account.id, PermissionLevel.OWNER, authorized.id))

        assertThrows<AccountAccessDeniedException> {
            authenticateHandler.handle(AuthenticateCommand("jorge@example.com", "Qlic2026!", owner.account.id))
        }
    }
}
