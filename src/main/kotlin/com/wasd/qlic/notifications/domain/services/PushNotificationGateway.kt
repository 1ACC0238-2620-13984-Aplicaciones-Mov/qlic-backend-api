package com.wasd.qlic.notifications.domain.services

import com.wasd.qlic.notifications.domain.model.PushDeliveryStatus
import com.wasd.qlic.notifications.domain.model.PushMessage

/** Outcome of a single delivery request to the push provider. */
data class GatewayResult(
    val status: PushDeliveryStatus,
    val providerMessageId: String? = null,
    val errorCode: String? = null
)

/** Domain port: external push provider (Firebase Cloud Messaging in production). */
interface PushNotificationGateway {
    fun send(token: String, message: PushMessage): GatewayResult
}
