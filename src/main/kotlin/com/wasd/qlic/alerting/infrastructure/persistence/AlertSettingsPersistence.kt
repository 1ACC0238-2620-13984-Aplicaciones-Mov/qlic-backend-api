package com.wasd.qlic.alerting.infrastructure.persistence

import com.wasd.qlic.alerting.domain.model.*
import com.wasd.qlic.alerting.domain.repositories.*
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "consumption_thresholds")
class ConsumptionThresholdEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val accountId: UUID = UUID.randomUUID(),
    @Column(nullable = false) val waterPointId: UUID = UUID.randomUUID(),
    @Column(nullable = false) val period: String = "HOURLY",
    @Column(nullable = true, precision = 10, scale = 2) val customLimitLitersPerHour: BigDecimal? = null,
    @Column(nullable = false, precision = 10, scale = 2) val baselineLitersPerHour: BigDecimal = BigDecimal("100.00"),
    @Enumerated(EnumType.STRING) @Column(nullable = false) val source: ThresholdSource = ThresholdSource.SYSTEM_CALCULATED,
    @Column(nullable = false) val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain() = ConsumptionThreshold(
        id = id,
        accountId = accountId,
        waterPointId = waterPointId,
        period = period,
        customLimitLitersPerHour = customLimitLitersPerHour,
        baselineLitersPerHour = baselineLitersPerHour,
        source = source,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(d: ConsumptionThreshold) = ConsumptionThresholdEntity(
            id = d.id,
            accountId = d.accountId,
            waterPointId = d.waterPointId,
            period = d.period,
            customLimitLitersPerHour = d.customLimitLitersPerHour,
            baselineLitersPerHour = d.baselineLitersPerHour,
            source = d.source,
            updatedAt = d.updatedAt
        )
    }
}

@Entity
@Table(name = "notification_preferences")
class NotificationPreferenceEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true) val accountId: UUID = UUID.randomUUID(),
    @Column(nullable = false) val groupingWindowMinutes: Int = 30,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val minimumUrgency: UrgencyLevel = UrgencyLevel.INFORMATIONAL,
    @Column(nullable = false) val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain() = NotificationPreference(
        id = id,
        accountId = accountId,
        groupingWindowMinutes = groupingWindowMinutes,
        minimumUrgency = minimumUrgency,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(d: NotificationPreference) = NotificationPreferenceEntity(
            id = d.id,
            accountId = d.accountId,
            groupingWindowMinutes = d.groupingWindowMinutes,
            minimumUrgency = d.minimumUrgency,
            updatedAt = d.updatedAt
        )
    }
}

@Entity
@Table(name = "authorized_alert_recipients")
class AuthorizedAlertRecipientEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val accountId: UUID = UUID.randomUUID(),
    @Column(nullable = false) val fullName: String = "",
    @Column(nullable = false) val email: String = "",
    @Column(nullable = false) val devicePushToken: String = "",
    @Column(nullable = false) val enabled: Boolean = true,
    @Column(nullable = true) val lastNotifiedAt: LocalDateTime? = null
) {
    fun toDomain() = AuthorizedAlertRecipient(
        id = id,
        accountId = accountId,
        fullName = fullName,
        email = email,
        devicePushToken = devicePushToken,
        enabled = enabled,
        lastNotifiedAt = lastNotifiedAt
    )

    companion object {
        fun fromDomain(d: AuthorizedAlertRecipient) = AuthorizedAlertRecipientEntity(
            id = d.id,
            accountId = d.accountId,
            fullName = d.fullName,
            email = d.email,
            devicePushToken = d.devicePushToken,
            enabled = d.enabled,
            lastNotifiedAt = d.lastNotifiedAt
        )
    }
}

@Repository
interface SpringDataConsumptionThresholdRepository : JpaRepository<ConsumptionThresholdEntity, UUID> {
    fun findByAccountIdAndWaterPointId(accountId: UUID, waterPointId: UUID): ConsumptionThresholdEntity?
    fun findAllByAccountId(accountId: UUID): List<ConsumptionThresholdEntity>
}

@Repository
interface SpringDataNotificationPreferenceRepository : JpaRepository<NotificationPreferenceEntity, UUID> {
    fun findByAccountId(accountId: UUID): NotificationPreferenceEntity?
}

@Repository
interface SpringDataAuthorizedRecipientRepository : JpaRepository<AuthorizedAlertRecipientEntity, UUID> {
    fun findAllByAccountId(accountId: UUID): List<AuthorizedAlertRecipientEntity>
    fun findAllByAccountIdAndEnabledTrue(accountId: UUID): List<AuthorizedAlertRecipientEntity>
}

@Component
class JpaConsumptionThresholdRepositoryImpl(
    private val jpa: SpringDataConsumptionThresholdRepository
) : ConsumptionThresholdRepository {
    override fun save(threshold: ConsumptionThreshold): ConsumptionThreshold =
        jpa.save(ConsumptionThresholdEntity.fromDomain(threshold)).toDomain()

    override fun findByAccountAndWaterPoint(accountId: UUID, waterPointId: UUID): ConsumptionThreshold? =
        jpa.findByAccountIdAndWaterPointId(accountId, waterPointId)?.toDomain()

    override fun findAllByAccountId(accountId: UUID): List<ConsumptionThreshold> =
        jpa.findAllByAccountId(accountId).map { it.toDomain() }
}

@Component
class JpaNotificationPreferenceRepositoryImpl(
    private val jpa: SpringDataNotificationPreferenceRepository
) : NotificationPreferenceRepository {
    override fun save(preference: NotificationPreference): NotificationPreference =
        jpa.save(NotificationPreferenceEntity.fromDomain(preference)).toDomain()

    override fun findByAccountId(accountId: UUID): NotificationPreference? =
        jpa.findByAccountId(accountId)?.toDomain()
}

@Component
class JpaAuthorizedAlertRecipientRepositoryImpl(
    private val jpa: SpringDataAuthorizedRecipientRepository
) : AuthorizedAlertRecipientRepository {
    override fun save(recipient: AuthorizedAlertRecipient): AuthorizedAlertRecipient =
        jpa.save(AuthorizedAlertRecipientEntity.fromDomain(recipient)).toDomain()

    override fun findById(id: UUID): AuthorizedAlertRecipient? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findAllByAccountId(accountId: UUID): List<AuthorizedAlertRecipient> =
        jpa.findAllByAccountId(accountId).map { it.toDomain() }

    override fun findEnabledByAccountId(accountId: UUID): List<AuthorizedAlertRecipient> =
        jpa.findAllByAccountIdAndEnabledTrue(accountId).map { it.toDomain() }
}