package com.wasd.qlic.notifications.application

import com.wasd.qlic.notifications.domain.exceptions.DeviceTokenNotFoundException
import com.wasd.qlic.notifications.domain.model.DevicePlatform
import com.wasd.qlic.notifications.domain.model.DeviceToken
import com.wasd.qlic.notifications.domain.model.NotificationCategory
import com.wasd.qlic.notifications.domain.model.PushDelivery
import com.wasd.qlic.notifications.domain.model.PushDeliveryStatus
import com.wasd.qlic.notifications.domain.model.PushMessage
import com.wasd.qlic.notifications.domain.repositories.DeviceTokenRepository
import com.wasd.qlic.notifications.domain.repositories.PushDeliveryRepository
import com.wasd.qlic.notifications.domain.services.GatewayResult
import com.wasd.qlic.notifications.domain.services.PushNotificationGateway
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class PushNotificationServiceTest {

    private class InMemoryDeviceTokens : DeviceTokenRepository {
        val storage = mutableMapOf<UUID, DeviceToken>()
        override fun save(deviceToken: DeviceToken) = deviceToken.also { storage[it.id] = it }
        override fun findByToken(token: String) = storage.values.firstOrNull { it.token == token }
        override fun findByAccountId(accountId: UUID) = storage.values.filter { it.accountId == accountId }
        override fun delete(deviceToken: DeviceToken) {
            storage.remove(deviceToken.id)
        }
    }

    private class InMemoryDeliveries : PushDeliveryRepository {
        val storage = mutableListOf<PushDelivery>()
        override fun save(delivery: PushDelivery) = delivery.also { storage.add(it) }
        override fun findRecentByAccountId(accountId: UUID, limit: Int) =
            storage.filter { it.accountId == accountId }.sortedByDescending { it.attemptedAt }.take(limit)
    }

    /** Accepts every token except those starting with "expired", which the provider reports as unregistered. */
    private class FakeProvider : PushNotificationGateway {
        val sentTo = mutableListOf<String>()
        override fun send(token: String, message: PushMessage): GatewayResult {
            sentTo += token
            return if (token.startsWith("expired")) GatewayResult(PushDeliveryStatus.INVALID_TOKEN, errorCode = "UNREGISTERED")
            else GatewayResult(PushDeliveryStatus.SENT, providerMessageId = "projects/qlic/messages/${sentTo.size}")
        }
    }

    private val accountId = UUID.randomUUID()
    private val message = PushMessage("Nivel crítico en Tanque", "El tanque está al 18%", NotificationCategory.TANK_LEVEL_LOW)
    private lateinit var tokens: InMemoryDeviceTokens
    private lateinit var deliveries: InMemoryDeliveries
    private lateinit var provider: FakeProvider
    private lateinit var service: PushNotificationService

    @BeforeEach
    fun setUp() {
        tokens = InMemoryDeviceTokens()
        deliveries = InMemoryDeliveries()
        provider = FakeProvider()
        service = PushNotificationService(tokens, deliveries, provider)
    }

    @Test
    @DisplayName("TS06 - Scenario 1: the provider accepts the message and the delivery is recorded")
    fun shouldRecordSuccessfulDelivery() {
        service.registerToken(accountId, "token-phone-1", DevicePlatform.ANDROID)

        val result = service.sendToAccount(accountId, message)

        assertEquals(1, result.size)
        assertEquals(PushDeliveryStatus.SENT, result[0].status)
        assertNotNull(result[0].providerMessageId)
        assertEquals(1, deliveries.storage.size)
        assertNotNull(tokens.findByToken("token-phone-1")!!.lastSentAt)
    }

    @Test
    @DisplayName("TS06 - Scenario 2: a token rejected by the provider is recorded and no longer used")
    fun shouldInvalidateRejectedToken() {
        service.registerToken(accountId, "expired-token", DevicePlatform.ANDROID)
        service.registerToken(accountId, "token-phone-2", DevicePlatform.ANDROID)

        val first = service.sendToAccount(accountId, message)
        assertEquals(setOf(PushDeliveryStatus.INVALID_TOKEN, PushDeliveryStatus.SENT), first.map { it.status }.toSet())
        assertFalse(tokens.findByToken("expired-token")!!.active)
        assertNotNull(tokens.findByToken("expired-token")!!.invalidatedAt)

        provider.sentTo.clear()
        service.sendToAccount(accountId, message)
        assertEquals(listOf("token-phone-2"), provider.sentTo)
    }

    @Test
    @DisplayName("Registering a token again moves it to the new account and reactivates it")
    fun shouldReactivateTokenOnRegistration() {
        service.registerToken(accountId, "expired-shared", DevicePlatform.ANDROID)
        service.sendToAccount(accountId, message)
        val otherAccount = UUID.randomUUID()

        val token = service.registerToken(otherAccount, "expired-shared", DevicePlatform.IOS)

        assertTrue(token.active)
        assertEquals(otherAccount, token.accountId)
        assertEquals(1, tokens.storage.size)
    }

    @Test
    @DisplayName("US19: the tokens of the authorized recipients receive the alert once, without duplicates")
    fun shouldSendToExtraTokensWithoutDuplicates() {
        service.registerToken(accountId, "token-owner", DevicePlatform.ANDROID)

        service.sendToAccount(accountId, message, listOf("token-owner", "token-recipient", "token-recipient", " "))

        assertEquals(listOf("token-owner", "token-recipient"), provider.sentTo)
    }

    @Test
    @DisplayName("A token of another account cannot be unregistered")
    fun shouldNotUnregisterTokenOfAnotherAccount() {
        service.registerToken(accountId, "token-phone-3", DevicePlatform.ANDROID)

        assertThrows(DeviceTokenNotFoundException::class.java) { service.unregisterToken(UUID.randomUUID(), "token-phone-3") }
    }
}
