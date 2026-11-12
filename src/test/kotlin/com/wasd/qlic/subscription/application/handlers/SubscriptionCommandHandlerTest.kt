package com.wasd.qlic.subscriptions.application.handlers

import com.wasd.qlic.subscriptions.domain.model.*
import com.wasd.qlic.subscriptions.domain.repositories.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class SubscriptionCommandHandlerTest {

    private lateinit var subscriptionRepo: SubscriptionRepository
    private lateinit var paymentRepo: PaymentRecordRepository
    private lateinit var handler: SubscriptionCommandHandler

    @BeforeEach
    fun setUp() {
        subscriptionRepo = object : SubscriptionRepository {
            private val store = mutableMapOf<UUID, Subscription>()
            override fun save(subscription: Subscription): Subscription {
                store[subscription.id] = subscription
                return subscription
            }
            override fun findById(id: UUID): Subscription? = store[id]
            override fun findByAccountId(accountId: UUID): Subscription? =
                store.values.find { it.accountId == accountId }
        }

        paymentRepo = object : PaymentRecordRepository {
            private val store = mutableListOf<PaymentRecord>()
            override fun save(payment: PaymentRecord): PaymentRecord {
                store.add(payment)
                return payment
            }
            override fun findAllByAccountId(accountId: UUID): List<PaymentRecord> =
                store.filter { it.accountId == accountId }
        }

        handler = SubscriptionCommandHandler(subscriptionRepo, paymentRepo)
    }

    // US31 - Escenario 1: Contratación exitosa
    @Test
    fun `should activate subscription and enable features when payment is confirmed`() {
        val accountId = UUID.randomUUID()
        val result = handler.purchaseSubscription(
            PurchaseSubscriptionCommand(
                accountId = accountId,
                plan = PlanType.PYME_BUSINESS,
                paymentMethod = "VISA",
                simulatePaymentRejected = false
            )
        )

        assertNotNull(result.subscription)
        assertEquals(SubscriptionStatus.ACTIVE, result.subscription?.status)
        assertTrue(result.subscription!!.hasPaidFeaturesAccess())
        assertEquals(PaymentStatus.CONFIRMED, result.paymentRecord.status)
    }

    // US31 - Escenario 2: Pago rechazado
    @Test
    fun `should not activate subscription when payment is rejected`() {
        val accountId = UUID.randomUUID()
        val result = handler.purchaseSubscription(
            PurchaseSubscriptionCommand(
                accountId = accountId,
                plan = PlanType.HOME_PRO,
                paymentMethod = "VISA",
                simulatePaymentRejected = true
            )
        )

        assertNull(result.subscription)
        assertEquals(PaymentStatus.REJECTED, result.paymentRecord.status)
        assertNotNull(result.paymentRecord.rejectionReason)
    }

    // US32 - Escenario 1 y Regla de negocio: Activación única de periodo de prueba
    @Test
    fun `should activate trial once and reject second trial request for same subscriber`() {
        val accountId = UUID.randomUUID()
        val trial = handler.startFreeTrial(StartTrialCommand(accountId))

        assertEquals(SubscriptionStatus.TRIAL_ACTIVE, trial.status)
        assertTrue(trial.trialUsed)
        assertTrue(trial.hasPaidFeaturesAccess())

        assertThrows(IllegalStateException::class.java) {
            handler.startFreeTrial(StartTrialCommand(accountId))
        }
    }

    // US32 - Escenario 2: Término del periodo de prueba restringe funcionalidades pagadas
    @Test
    fun `should restrict paid features when trial period expires without purchasing a plan`() {
        val accountId = UUID.randomUUID()
        val trial = handler.startFreeTrial(StartTrialCommand(accountId))

        val afterExpiration = trial.currentPeriodEndsAt.plusDays(1)
        val evaluated = handler.getCurrentSubscription(accountId, afterExpiration)

        assertEquals(SubscriptionStatus.TRIAL_EXPIRED, evaluated?.status)
        assertFalse(evaluated!!.hasPaidFeaturesAccess(afterExpiration))
    }

    // US33 - Escenario 1 y 2: Cambio de plan para siguiente periodo y cancelación con acceso vigente
    @Test
    fun `should schedule plan change for next period and keep access until period end upon cancellation`() {
        val accountId = UUID.randomUUID()
        val sub = handler.purchaseSubscription(
            PurchaseSubscriptionCommand(accountId, PlanType.HOME_BASIC, "YAPE", false)
        ).subscription!!

        val changed = handler.changePlan(ChangePlanCommand(sub.id, PlanType.HOME_PRO))
        assertEquals(PlanType.HOME_BASIC, changed.currentPlan)
        assertEquals(PlanType.HOME_PRO, changed.nextBillingPlan)

        val cancelled = handler.cancelSubscription(CancelSubscriptionCommand(sub.id))
        assertEquals(SubscriptionStatus.CANCELLED_PENDING_EXPIRATION, cancelled.status)
        assertFalse(cancelled.autoRenew)
        assertTrue(cancelled.hasPaidFeaturesAccess())
    }

    // US34 - Escenario 1: Consultar historial de pagos con fecha, monto, plan y estado
    @Test
    fun `should return payment history with date, amount, plan, and status`() {
        val accountId = UUID.randomUUID()
        handler.purchaseSubscription(
            PurchaseSubscriptionCommand(accountId, PlanType.HOME_BASIC, "YAPE", true)
        )
        handler.purchaseSubscription(
            PurchaseSubscriptionCommand(accountId, PlanType.HOME_BASIC, "PLIN", false)
        )

        val history = handler.getPaymentHistory(accountId)

        assertEquals(2, history.size)
        assertEquals(BigDecimal("29.90"), history[0].amountSoles)
        assertEquals(PlanType.HOME_BASIC, history[0].planAssociated)
    }
}