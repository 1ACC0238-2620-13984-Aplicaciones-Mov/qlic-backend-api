package com.wasd.qlic.subscriptions.application.handlers

import com.wasd.qlic.subscriptions.domain.model.*
import com.wasd.qlic.subscriptions.domain.repositories.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

data class PurchaseSubscriptionCommand(
    val accountId: UUID,
    val plan: PlanType,
    val paymentMethod: String,
    val simulatePaymentRejected: Boolean = false
)

data class StartTrialCommand(
    val accountId: UUID
)

data class ChangePlanCommand(
    val subscriptionId: UUID,
    val newPlan: PlanType
)

data class CancelSubscriptionCommand(
    val subscriptionId: UUID
)

data class PurchaseResult(
    val subscription: Subscription?,
    val paymentRecord: PaymentRecord,
    val message: String
)

@Service
class SubscriptionCommandHandler(
    private val subscriptionRepository: SubscriptionRepository,
    private val paymentRepository: PaymentRecordRepository
) {
    // US31: Contratar un plan de suscripción (Escenario 1: Confirmado | Escenario 2: Rechazado)
    fun purchaseSubscription(command: PurchaseSubscriptionCommand): PurchaseResult {
        require(command.plan != PlanType.TRIAL) { "Para periodo de prueba utilice el endpoint de trial" }
        val existing = subscriptionRepository.findByAccountId(command.accountId)

        if (command.simulatePaymentRejected) {
            val rejectedPayment = paymentRepository.save(
                PaymentRecord(
                    accountId = command.accountId,
                    subscriptionId = existing?.id,
                    planAssociated = command.plan,
                    amountSoles = command.plan.monthlyPriceSoles,
                    status = PaymentStatus.REJECTED,
                    paymentMethod = command.paymentMethod,
                    rejectionReason = "El pago no pudo procesarse por fondos insuficientes o rechazo del emisor"
                )
            )
            return PurchaseResult(
                subscription = existing,
                paymentRecord = rejectedPayment,
                message = "El pago no pudo procesarse. La suscripción no fue activada."
            )
        }

        val now = LocalDateTime.now()
        val subscription = Subscription(
            id = existing?.id ?: UUID.randomUUID(),
            accountId = command.accountId,
            currentPlan = command.plan,
            nextBillingPlan = null,
            status = SubscriptionStatus.ACTIVE,
            trialUsed = existing?.trialUsed ?: false,
            startedAt = now,
            currentPeriodEndsAt = now.plusDays(30),
            autoRenew = true,
            updatedAt = now
        )
        val savedSub = subscriptionRepository.save(subscription)

        val confirmedPayment = paymentRepository.save(
            PaymentRecord(
                accountId = command.accountId,
                subscriptionId = savedSub.id,
                planAssociated = command.plan,
                amountSoles = command.plan.monthlyPriceSoles,
                status = PaymentStatus.CONFIRMED,
                paymentMethod = command.paymentMethod
            )
        )

        return PurchaseResult(
            subscription = savedSub,
            paymentRecord = confirmedPayment,
            message = "Suscripción activada y funcionalidades del plan habilitadas."
        )
    }

    // US32: Acceder a un periodo de prueba (Regla: una única vez por suscriptor)
    fun startFreeTrial(command: StartTrialCommand): Subscription {
        val existing = subscriptionRepository.findByAccountId(command.accountId)
        if (existing != null && existing.trialUsed) {
            throw IllegalStateException("El periodo de prueba se otorga una única vez por suscriptor")
        }

        val now = LocalDateTime.now()
        val trialSub = Subscription(
            id = existing?.id ?: UUID.randomUUID(),
            accountId = command.accountId,
            currentPlan = PlanType.TRIAL,
            status = SubscriptionStatus.TRIAL_ACTIVE,
            trialUsed = true,
            startedAt = now,
            currentPeriodEndsAt = now.plusDays(14),
            autoRenew = false,
            updatedAt = now
        )
        return subscriptionRepository.save(trialSub)
    }

    // US32 - Escenario 2: Evaluar estado actual y restringir si el trial concluyó sin plan contratado
    fun getCurrentSubscription(accountId: UUID, evaluationTime: LocalDateTime = LocalDateTime.now()): Subscription? {
        val sub = subscriptionRepository.findByAccountId(accountId) ?: return null
        val hadActiveTrial = sub.status == SubscriptionStatus.TRIAL_ACTIVE
        sub.hasPaidFeaturesAccess(evaluationTime)
        if (hadActiveTrial && sub.status == SubscriptionStatus.TRIAL_EXPIRED) {
            return subscriptionRepository.save(sub)
        }
        return sub
    }

    // US33 - Escenario 1: Cambiar de plan a partir del siguiente periodo de facturación
    fun changePlan(command: ChangePlanCommand): Subscription {
        val sub = subscriptionRepository.findById(command.subscriptionId)
            ?: throw IllegalArgumentException("Suscripción no encontrada: ${command.subscriptionId}")
        sub.schedulePlanChange(command.newPlan)
        return subscriptionRepository.save(sub)
    }

    // US33 - Escenario 2: Cancelar suscripción manteniendo acceso hasta el término del periodo pagado
    fun cancelSubscription(command: CancelSubscriptionCommand): Subscription {
        val sub = subscriptionRepository.findById(command.subscriptionId)
            ?: throw IllegalArgumentException("Suscripción no encontrada: ${command.subscriptionId}")
        sub.cancelSubscription()
        return subscriptionRepository.save(sub)
    }

    // US34: Consultar el historial de pagos
    fun getPaymentHistory(accountId: UUID): List<PaymentRecord> =
        paymentRepository.findAllByAccountId(accountId)
}