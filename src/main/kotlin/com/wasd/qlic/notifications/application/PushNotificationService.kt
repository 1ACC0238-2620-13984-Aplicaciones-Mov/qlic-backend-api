package com.wasd.qlic.notifications.application

import com.wasd.qlic.notifications.domain.exceptions.DeviceTokenNotFoundException
import com.wasd.qlic.notifications.domain.model.DevicePlatform
import com.wasd.qlic.notifications.domain.model.DeviceToken
import com.wasd.qlic.notifications.domain.model.PushDelivery
import com.wasd.qlic.notifications.domain.model.PushDeliveryStatus
import com.wasd.qlic.notifications.domain.model.PushMessage
import com.wasd.qlic.notifications.domain.repositories.DeviceTokenRepository
import com.wasd.qlic.notifications.domain.repositories.PushDeliveryRepository
import com.wasd.qlic.notifications.domain.services.PushNotificationGateway
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * TS06: registers the device tokens of the subscriber and sends the push notifications through the
 * external provider. Every attempt is recorded (Scenario 1) and a token rejected by the provider is marked
 * as no longer valid so it is not used again (Scenario 2).
 */
@Service
class PushNotificationService(
    private val tokenRepository: DeviceTokenRepository,
    private val deliveryRepository: PushDeliveryRepository,
    private val gateway: PushNotificationGateway
) {
    private val log = LoggerFactory.getLogger(PushNotificationService::class.java)

    @Transactional
    fun registerToken(accountId: UUID, token: String, platform: DevicePlatform): DeviceToken {
        val trimmed = token.trim()
        val deviceToken = tokenRepository.findByToken(trimmed)?.also { it.register(accountId, platform) }
            ?: DeviceToken(accountId = accountId, token = trimmed, platform = platform)
        return tokenRepository.save(deviceToken)
    }

    @Transactional
    fun unregisterToken(accountId: UUID, token: String) {
        val deviceToken = tokenRepository.findByToken(token.trim())?.takeIf { it.accountId == accountId }
            ?: throw DeviceTokenNotFoundException()
        tokenRepository.delete(deviceToken)
    }

    @Transactional(readOnly = true)
    fun tokens(accountId: UUID): List<DeviceToken> = tokenRepository.findByAccountId(accountId)

    @Transactional(readOnly = true)
    fun deliveries(accountId: UUID, limit: Int = 50): List<PushDelivery> =
        deliveryRepository.findRecentByAccountId(accountId, limit)

    /**
     * Sends the message to every active device of the account and, for leak alerts, to the tokens of the
     * authorized recipients (US19). Runs in its own transaction so that it can be called after the
     * transaction that raised the alert has committed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun sendToAccount(accountId: UUID, message: PushMessage, extraTokens: Collection<String> = emptyList()): List<PushDelivery> {
        val registered = tokenRepository.findByAccountId(accountId).filter { it.active }
        val registeredValues = registered.map { it.token }.toSet()
        val deliveries = registered.map { deliver(accountId, it, it.token, message) } +
            extraTokens.map { it.trim() }.filter { it.isNotBlank() && it !in registeredValues }.distinct()
                .map { deliver(accountId, null, it, message) }
        log.info("Push {} for account {}: {}", message.category, accountId, deliveries.groupingBy { it.status }.eachCount())
        return deliveries
    }

    private fun deliver(accountId: UUID, deviceToken: DeviceToken?, token: String, message: PushMessage): PushDelivery {
        val result = try {
            gateway.send(token, message)
        } catch (exception: RuntimeException) {
            log.warn("Push provider failure for account {}: {}", accountId, exception.message)
            com.wasd.qlic.notifications.domain.services.GatewayResult(PushDeliveryStatus.FAILED, errorCode = exception.javaClass.simpleName)
        }
        deviceToken?.let {
            when (result.status) {
                PushDeliveryStatus.SENT -> it.markSent()
                PushDeliveryStatus.INVALID_TOKEN -> it.invalidate()
                else -> Unit
            }
            tokenRepository.save(it)
        }
        return deliveryRepository.save(
            PushDelivery(
                accountId = accountId,
                deviceTokenId = deviceToken?.id,
                maskedToken = "…" + token.takeLast(8),
                category = message.category,
                title = message.title,
                status = result.status,
                providerMessageId = result.providerMessageId,
                errorCode = result.errorCode
            )
        )
    }
}
