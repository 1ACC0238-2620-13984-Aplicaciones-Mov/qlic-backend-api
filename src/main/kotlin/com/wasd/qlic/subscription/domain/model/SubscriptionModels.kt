package com.wasd.qlic.subscriptions.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class PlanType(val monthlyPriceSoles: BigDecimal, val maxWaterPoints: Int) {
    TRIAL(BigDecimal("0.00"), 2),
    HOME_BASIC(BigDecimal("29.90"), 3),
    HOME_PRO(BigDecimal("49.90"), 6),
    PYME_BUSINESS(BigDecimal("99.90"), 15),
    PYME_ENTERPRISE(BigDecimal("199.90"), 50)
}

enum class SubscriptionStatus {
    TRIAL_ACTIVE,
    TRIAL_EXPIRED,
    ACTIVE,
    CANCELLED_PENDING_EXPIRATION,
    PAYMENT_REJECTED
}

enum class PaymentStatus {
    CONFIRMED,
    REJECTED
}

class Subscription(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    var currentPlan: PlanType,
    var nextBillingPlan: PlanType? = null,
    var status: SubscriptionStatus,
    val trialUsed: Boolean = false,
    val startedAt: LocalDateTime = LocalDateTime.now(),
    var currentPeriodEndsAt: LocalDateTime,
    var autoRenew: Boolean = true,
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // Verifica si el suscriptor tiene acceso habilitado a las funcionalidades pagadas/monitoreo
    fun hasPaidFeaturesAccess(now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (now.isAfter(currentPeriodEndsAt)) {
            if (status == SubscriptionStatus.TRIAL_ACTIVE) {
                status = SubscriptionStatus.TRIAL_EXPIRED
            }
            return false
        }
        return status == SubscriptionStatus.ACTIVE ||
                status == SubscriptionStatus.TRIAL_ACTIVE ||
                status == SubscriptionStatus.CANCELLED_PENDING_EXPIRATION
    }

    // US33 - Escenario 1: Cambio de plan aplicable a partir del siguiente periodo de facturación
    fun schedulePlanChange(newPlan: PlanType) {
        require(status == SubscriptionStatus.ACTIVE) {
            "Solo se puede cambiar de plan cuando la suscripción está activa"
        }
        require(newPlan != PlanType.TRIAL) {
            "No se puede cambiar hacia un plan de prueba"
        }
        this.nextBillingPlan = newPlan
        this.updatedAt = LocalDateTime.now()
    }

    // US33 - Escenario 2: Cancelación manteniendo acceso hasta el término del periodo pagado sin nuevos cobros
    fun cancelSubscription() {
        require(status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIAL_ACTIVE) {
            "La suscripción no se encuentra activa para ser cancelada"
        }
        this.status = SubscriptionStatus.CANCELLED_PENDING_EXPIRATION
        this.autoRenew = false
        this.nextBillingPlan = null
        this.updatedAt = LocalDateTime.now()
    }
}

class PaymentRecord(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val subscriptionId: UUID?,
    val planAssociated: PlanType,
    val amountSoles: BigDecimal,
    val status: PaymentStatus,
    val paymentMethod: String,
    val rejectionReason: String? = null,
    val paidAt: LocalDateTime = LocalDateTime.now()
)