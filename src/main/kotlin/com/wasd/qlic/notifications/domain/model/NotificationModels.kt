package com.wasd.qlic.notifications.domain.model

import java.time.Instant
import java.util.UUID

enum class DevicePlatform { ANDROID, IOS, WEB }

/** Reason of a push notification, used by the app to open the right screen. */
enum class NotificationCategory { LEAK_ALERT, TANK_LEVEL_LOW, DEVICE_OFFLINE, TEST }

/** Result of a delivery attempt reported by the push provider (TS06). */
enum class PushDeliveryStatus {
    /** Scenario 1: the provider accepted the message. */
    SENT,

    /** Scenario 2: the provider rejected the token; it is marked as no longer valid. */
    INVALID_TOKEN,

    /** Temporary failure of the provider; the token is kept. */
    FAILED,

    /** Push is not configured in this environment (no Firebase credentials). */
    DISABLED
}

/**
 * Entity: registration token of the mobile app on a device of the subscriber. The same physical device
 * always has one token, so registering it again moves it to the account of the session and reactivates it.
 */
class DeviceToken(
    val id: UUID = UUID.randomUUID(),
    accountId: UUID,
    val token: String,
    platform: DevicePlatform,
    active: Boolean = true,
    registeredAt: Instant = Instant.now(),
    lastSentAt: Instant? = null,
    invalidatedAt: Instant? = null
) {
    var accountId: UUID = accountId
        private set
    var platform: DevicePlatform = platform
        private set
    var active: Boolean = active
        private set
    var registeredAt: Instant = registeredAt
        private set
    var lastSentAt: Instant? = lastSentAt
        private set
    var invalidatedAt: Instant? = invalidatedAt
        private set

    init {
        require(token.isNotBlank()) { "The token is required" }
    }

    fun register(accountId: UUID, platform: DevicePlatform) {
        this.accountId = accountId
        this.platform = platform
        active = true
        invalidatedAt = null
        registeredAt = Instant.now()
    }

    fun markSent() {
        lastSentAt = Instant.now()
    }

    /** TS06 - Scenario 2: the provider rejected the token. */
    fun invalidate() {
        active = false
        invalidatedAt = Instant.now()
    }

    /** Only the end of the token is exposed, it is a credential of the device. */
    val maskedToken: String get() = "…" + token.takeLast(8)
}

/** Content of a push notification. data travels to the app for navigation (category, ids). */
data class PushMessage(
    val title: String,
    val body: String,
    val category: NotificationCategory,
    val data: Map<String, String> = emptyMap()
)

/** Record of each delivery attempt (TS06 - Scenario 1: the system registers the delivery). */
data class PushDelivery(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val deviceTokenId: UUID?,
    val maskedToken: String,
    val category: NotificationCategory,
    val title: String,
    val status: PushDeliveryStatus,
    val providerMessageId: String? = null,
    val errorCode: String? = null,
    val attemptedAt: Instant = Instant.now()
)
