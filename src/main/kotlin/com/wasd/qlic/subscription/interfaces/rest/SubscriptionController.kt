package com.wasd.qlic.subscriptions.interfaces.rest

import com.wasd.qlic.subscriptions.application.handlers.*
import com.wasd.qlic.subscriptions.domain.model.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class PurchaseSubscriptionRequest(
    val accountId: UUID,
    val plan: PlanType,
    val paymentMethod: String = "YAPE_PLIN_OR_CARD",
    val simulatePaymentRejected: Boolean = false
)

data class StartTrialRequest(
    val accountId: UUID
)

data class ChangePlanRequest(
    val newPlan: PlanType
)

data class SubscriptionResource(
    val id: UUID,
    val accountId: UUID,
    val currentPlan: PlanType,
    val nextBillingPlan: PlanType?,
    val status: SubscriptionStatus,
    val trialUsed: Boolean,
    val paidFeaturesEnabled: Boolean,
    val maxWaterPointsAllowed: Int,
    val startedAt: LocalDateTime,
    val currentPeriodEndsAt: LocalDateTime,
    val autoRenew: Boolean
)

data class PaymentRecordResource(
    val id: UUID,
    val accountId: UUID,
    val subscriptionId: UUID?,
    val planAssociated: PlanType,
    val amountSoles: BigDecimal,
    val status: PaymentStatus,
    val paymentMethod: String,
    val rejectionReason: String?,
    val paidAt: LocalDateTime
)

data class PurchaseSubscriptionResponse(
    val message: String,
    val subscription: SubscriptionResource?,
    val payment: PaymentRecordResource
)

@RestController
@RequestMapping("/api/v1/subscriptions")
class SubscriptionController(
    private val handler: SubscriptionCommandHandler
) {
    private fun Subscription.toResource() = SubscriptionResource(
        id = id,
        accountId = accountId,
        currentPlan = currentPlan,
        nextBillingPlan = nextBillingPlan,
        status = status,
        trialUsed = trialUsed,
        paidFeaturesEnabled = hasPaidFeaturesAccess(),
        maxWaterPointsAllowed = if (hasPaidFeaturesAccess()) currentPlan.maxWaterPoints else 0,
        startedAt = startedAt,
        currentPeriodEndsAt = currentPeriodEndsAt,
        autoRenew = autoRenew
    )

    private fun PaymentRecord.toResource() = PaymentRecordResource(
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

    // US31: Contratar un plan de suscripción
    @PostMapping
    fun purchaseSubscription(
        @RequestBody req: PurchaseSubscriptionRequest
    ): ResponseEntity<PurchaseSubscriptionResponse> {
        val result = handler.purchaseSubscription(
            PurchaseSubscriptionCommand(
                accountId = req.accountId,
                plan = req.plan,
                paymentMethod = req.paymentMethod,
                simulatePaymentRejected = req.simulatePaymentRejected
            )
        )
        val statusHttp = if (result.paymentRecord.status == PaymentStatus.CONFIRMED) {
            HttpStatus.CREATED
        } else {
            HttpStatus.PAYMENT_REQUIRED
        }
        return ResponseEntity.status(statusHttp).body(
            PurchaseSubscriptionResponse(
                message = result.message,
                subscription = result.subscription?.toResource(),
                payment = result.paymentRecord.toResource()
            )
        )
    }

    // US32: Activar periodo de prueba (una única vez por suscriptor)
    @PostMapping("/trial")
    fun startFreeTrial(@RequestBody req: StartTrialRequest): ResponseEntity<Any> {
        return try {
            val sub = handler.startFreeTrial(StartTrialCommand(req.accountId))
            ResponseEntity.status(HttpStatus.CREATED).body(sub.toResource())
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to ex.message))
        }
    }

    // US32: Consultar estado actual de suscripción y vigencia de funcionalidades
    @GetMapping("/current")
    fun getCurrentSubscription(@RequestParam accountId: UUID): ResponseEntity<SubscriptionResource> {
        val sub = handler.getCurrentSubscription(accountId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sub.toResource())
    }

    // US33 - Escenario 1: Cambiar el plan contratado para el siguiente periodo
    @PatchMapping("/{subscriptionId}/plan")
    fun changePlan(
        @PathVariable subscriptionId: UUID,
        @RequestBody req: ChangePlanRequest
    ): ResponseEntity<SubscriptionResource> {
        val updated = handler.changePlan(ChangePlanCommand(subscriptionId, req.newPlan))
        return ResponseEntity.ok(updated.toResource())
    }

    // US33 - Escenario 2: Cancelar el plan contratado manteniendo acceso hasta fin de periodo
    @PostMapping("/{subscriptionId}/cancel")
    fun cancelSubscription(@PathVariable subscriptionId: UUID): ResponseEntity<SubscriptionResource> {
        val cancelled = handler.cancelSubscription(CancelSubscriptionCommand(subscriptionId))
        return ResponseEntity.ok(cancelled.toResource())
    }

    // US34: Consultar el historial de pagos
    @GetMapping("/payments")
    fun getPaymentHistory(@RequestParam accountId: UUID): ResponseEntity<List<PaymentRecordResource>> {
        val payments = handler.getPaymentHistory(accountId).map { it.toResource() }
        return ResponseEntity.ok(payments)
    }
}