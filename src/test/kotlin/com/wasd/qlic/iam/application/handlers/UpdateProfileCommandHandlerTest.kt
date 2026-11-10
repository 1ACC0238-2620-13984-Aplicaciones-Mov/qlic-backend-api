package com.wasd.qlic.iam.application.handlers

import com.wasd.qlic.iam.application.commands.RegisterAccountCommand
import com.wasd.qlic.iam.application.commands.UpdateProfileCommand
import com.wasd.qlic.iam.domain.exceptions.PermissionDeniedException
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import com.wasd.qlic.iam.domain.services.AuthorizationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class UpdateProfileCommandHandlerTest {

    private val subscribers = InMemorySubscriberRepository()
    private val accounts = InMemoryAccountRepository()
    private lateinit var registered: RegisteredAccount
    private lateinit var handler: UpdateProfileCommandHandler

    @BeforeEach
    fun setUp() {
        registered = RegisterAccountCommandHandler(subscribers, accounts, FakePasswordHasher(), ApplicationEventPublisher { })
            .handle(RegisterAccountCommand("Luis Ramos", "luis.ramos@example.com", "Qlic2026!", "+51911111111", true))
        handler = UpdateProfileCommandHandler(subscribers, accounts, AuthorizationService())
    }

    private fun command(fullName: String, phone: String?, accountName: String?, permission: PermissionLevel = PermissionLevel.OWNER) =
        UpdateProfileCommand(registered.subscriber.id, registered.account.id, permission, fullName, phone, accountName)

    @Test
    @DisplayName("US09 - Scenario 1: valid data is registered and returned in the next queries")
    fun shouldUpdateProfileAndPremisesName() {
        handler.handle(command("Luis Ramos Díaz", "  ", "Restaurante Don Lucho"))

        val subscriber = subscribers.findById(registered.subscriber.id)!!
        assertEquals("Luis Ramos Díaz", subscriber.fullName)
        assertNull(subscriber.phone)
        assertEquals("Restaurante Don Lucho", accounts.findById(registered.account.id)!!.name)
    }

    @Test
    @DisplayName("US09 - Scenario 2: an empty mandatory field does not register the changes")
    fun shouldRejectEmptyFullName() {
        assertThrows<IllegalArgumentException> { handler.handle(command(" ", null, null)) }
        assertEquals("Luis Ramos", subscribers.findById(registered.subscriber.id)!!.fullName)
    }

    @Test
    @DisplayName("US09: only the owner can rename the premises")
    fun shouldRejectRenameByViewer() {
        assertThrows<PermissionDeniedException> {
            handler.handle(command("Luis Ramos", null, "Otro nombre", PermissionLevel.VIEWER))
        }
    }
}
