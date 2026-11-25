package com.wasd.qlic.notifications.domain.repositories

import com.wasd.qlic.notifications.domain.model.DeviceToken
import com.wasd.qlic.notifications.domain.model.PushDelivery
import java.util.UUID

interface DeviceTokenRepository {
    fun save(deviceToken: DeviceToken): DeviceToken
    fun findByToken(token: String): DeviceToken?
    fun findByAccountId(accountId: UUID): List<DeviceToken>
    fun delete(deviceToken: DeviceToken)
}

interface PushDeliveryRepository {
    fun save(delivery: PushDelivery): PushDelivery
    fun findRecentByAccountId(accountId: UUID, limit: Int): List<PushDelivery>
}
