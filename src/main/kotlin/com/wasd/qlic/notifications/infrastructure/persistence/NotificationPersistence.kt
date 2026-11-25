package com.wasd.qlic.notifications.infrastructure.persistence

import com.wasd.qlic.notifications.domain.model.DevicePlatform
import com.wasd.qlic.notifications.domain.model.DeviceToken
import com.wasd.qlic.notifications.domain.model.NotificationCategory
import com.wasd.qlic.notifications.domain.model.PushDelivery
import com.wasd.qlic.notifications.domain.model.PushDeliveryStatus
import com.wasd.qlic.notifications.domain.repositories.DeviceTokenRepository
import com.wasd.qlic.notifications.domain.repositories.PushDeliveryRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device_tokens", indexes = [Index(name = "idx_device_tokens_account", columnList = "account_id")])
class DeviceTokenEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(nullable = false, unique = true, length = 512) val token: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) val platform: DevicePlatform,
    @Column(nullable = false) val active: Boolean,
    @Column(name = "registered_at", nullable = false) val registeredAt: Instant,
    @Column(name = "last_sent_at") val lastSentAt: Instant?,
    @Column(name = "invalidated_at") val invalidatedAt: Instant?
)

@Entity
@Table(name = "push_deliveries", indexes = [Index(name = "idx_push_deliveries_account", columnList = "account_id, attempted_at")])
class PushDeliveryEntity(
    @Id val id: UUID,
    @Column(name = "account_id", nullable = false) val accountId: UUID,
    @Column(name = "device_token_id") val deviceTokenId: UUID?,
    @Column(name = "masked_token", nullable = false, length = 16) val maskedToken: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val category: NotificationCategory,
    @Column(nullable = false, length = 160) val title: String,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) val status: PushDeliveryStatus,
    @Column(name = "provider_message_id", length = 200) val providerMessageId: String?,
    @Column(name = "error_code", length = 80) val errorCode: String?,
    @Column(name = "attempted_at", nullable = false) val attemptedAt: Instant
)

@Repository
interface SpringDataDeviceTokenRepository : JpaRepository<DeviceTokenEntity, UUID> {
    fun findByToken(token: String): DeviceTokenEntity?
    fun findAllByAccountIdOrderByRegisteredAtDesc(accountId: UUID): List<DeviceTokenEntity>
}

@Repository
interface SpringDataPushDeliveryRepository : JpaRepository<PushDeliveryEntity, UUID> {
    fun findAllByAccountIdOrderByAttemptedAtDesc(accountId: UUID, pageable: org.springframework.data.domain.Pageable): List<PushDeliveryEntity>
}

@Component
class JpaDeviceTokenRepositoryImpl(
    private val jpa: SpringDataDeviceTokenRepository
) : DeviceTokenRepository {

    override fun save(deviceToken: DeviceToken): DeviceToken = toDomain(
        jpa.save(
            DeviceTokenEntity(
                deviceToken.id, deviceToken.accountId, deviceToken.token, deviceToken.platform, deviceToken.active,
                deviceToken.registeredAt, deviceToken.lastSentAt, deviceToken.invalidatedAt
            )
        )
    )

    override fun findByToken(token: String): DeviceToken? = jpa.findByToken(token)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<DeviceToken> =
        jpa.findAllByAccountIdOrderByRegisteredAtDesc(accountId).map { toDomain(it) }

    override fun delete(deviceToken: DeviceToken) = jpa.deleteById(deviceToken.id)

    private fun toDomain(entity: DeviceTokenEntity) = DeviceToken(
        id = entity.id,
        accountId = entity.accountId,
        token = entity.token,
        platform = entity.platform,
        active = entity.active,
        registeredAt = entity.registeredAt,
        lastSentAt = entity.lastSentAt,
        invalidatedAt = entity.invalidatedAt
    )
}

@Component
class JpaPushDeliveryRepositoryImpl(
    private val jpa: SpringDataPushDeliveryRepository
) : PushDeliveryRepository {

    override fun save(delivery: PushDelivery): PushDelivery {
        jpa.save(
            PushDeliveryEntity(
                delivery.id, delivery.accountId, delivery.deviceTokenId, delivery.maskedToken, delivery.category,
                delivery.title.take(160), delivery.status, delivery.providerMessageId?.take(200), delivery.errorCode?.take(80),
                delivery.attemptedAt
            )
        )
        return delivery
    }

    override fun findRecentByAccountId(accountId: UUID, limit: Int): List<PushDelivery> =
        jpa.findAllByAccountIdOrderByAttemptedAtDesc(accountId, PageRequest.of(0, limit)).map {
            PushDelivery(
                it.id, it.accountId, it.deviceTokenId, it.maskedToken, it.category, it.title, it.status,
                it.providerMessageId, it.errorCode, it.attemptedAt
            )
        }
}
