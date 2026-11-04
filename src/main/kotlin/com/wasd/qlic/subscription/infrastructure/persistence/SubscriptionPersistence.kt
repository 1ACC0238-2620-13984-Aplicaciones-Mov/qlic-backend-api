package com.wasd.qlic.subscriptions.infrastructure.persistence

import com.wasd.qlic.subscriptions.domain.model.*
import com.wasd.qlic.subscriptions.domain.repositories.*
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "subscriptions")
class SubscriptionEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true) val accountId: UUID = UUID.randomUUID(),
    @Enumerated(EnumType.STRING) @Column(nullable = false) val currentPlan: PlanType = PlanType.HOME_BASIC,
    @Enumerated(EnumType.STRING) @Column(nullable = true) val nextBillingPlan: PlanType? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    @Column(nullable = false) val trialUsed: Boolean = false,
    @Column(nullable = false) val startedAt: LocalDateTime = LocalDateTime.now(),
    @Column(nullable = false) val currentPeriodEndsAt: LocalDateTime = LocalDateTime.now().plusDays(30),
    @Column(nullable = false) val autoRenew: Boolean = true,
    @Column(nullable = false) val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain() = Subscription(
        id = id,
        accountId = accountId,
        currentPlan = currentPlan,
        nextBillingPlan = nextBillingPlan,
        status = status,
        trialUsed = trialUsed,
        startedAt = startedAt,
        currentPeriodEndsAt = currentPeriodEndsAt,
        autoRenew = autoRenew,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(d: Subscription) = SubscriptionEntity(
            id = d.id,
            accountId = d.accountId,
            currentPlan = d.currentPlan,
            nextBillingPlan = d.nextBillingPlan,
            status = d.status,
            trialUsed = d.trialUsed,
            startedAt = d.startedAt,
            currentPeriodEndsAt = d.currentPeriodEndsAt,
            autoRenew = d.autoRenew,
            updatedAt = d.updatedAt
        )
    }
}

@Entity
@Table(name = "payment_records")
class PaymentRecordEntity(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val accountId: UUID = UUID.randomUUID(),
    @Column(nullable = true) val subscriptionId: UUID? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val planAssociated: PlanType = PlanType.HOME_BASIC,
    @Column(nullable = false, precision = 10, scale = 2) val amountSoles: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val status: PaymentStatus = PaymentStatus.CONFIRMED,
    @Column(nullable = false) val paymentMethod: String = "CARD",
    @Column(nullable = true) val rejectionReason: String? = null,
    @Column(nullable = false) val paidAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain() = PaymentRecord(
        id = id,
        accountId = accountId,
        subscriptionId = subscriptionId,
        planAssociated = planAssociated,
        amountSoles = amountSoles,
        status = status,
        paymentMethod = paymentMethod,
        rejectionReason = rejectionReason,
        paidAt = paidAt
    )

    companion object {
        fun fromDomain(d: PaymentRecord) = PaymentRecordEntity(
            id = d.id,
            accountId = d.accountId,
            subscriptionId = d.subscriptionId,
            planAssociated = d.planAssociated,
            amountSoles = d.amountSoles,
            status = d.status,
            paymentMethod = d.paymentMethod,
            rejectionReason = d.rejectionReason,
            paidAt = d.paidAt
        )
    }
}

@Repository
interface SpringDataSubscriptionRepository : JpaRepository<SubscriptionEntity, UUID> {
    fun findByAccountId(accountId: UUID): SubscriptionEntity?
}

@Repository
interface SpringDataPaymentRecordRepository : JpaRepository<PaymentRecordEntity, UUID> {
    fun findAllByAccountIdOrderByPaidAtDesc(accountId: UUID): List<PaymentRecordEntity>
}

@Component
class JpaSubscriptionRepositoryImpl(
    private val jpa: SpringDataSubscriptionRepository
) : SubscriptionRepository {
    override fun save(subscription: Subscription): Subscription =
        jpa.save(SubscriptionEntity.fromDomain(subscription)).toDomain()

    override fun findById(id: UUID): Subscription? =
        jpa.findById(id).orElse(null)?.toDomain()

    override fun findByAccountId(accountId: UUID): Subscription? =
        jpa.findByAccountId(accountId)?.toDomain()
}

@Component
class JpaPaymentRecordRepositoryImpl(
    private val jpa: SpringDataPaymentRecordRepository
) : PaymentRecordRepository {
    override fun save(payment: PaymentRecord): PaymentRecord =
        jpa.save(PaymentRecordEntity.fromDomain(payment)).toDomain()

    override fun findAllByAccountId(accountId: UUID): List<PaymentRecord> =
        jpa.findAllByAccountIdOrderByPaidAtDesc(accountId).map { it.toDomain() }
}