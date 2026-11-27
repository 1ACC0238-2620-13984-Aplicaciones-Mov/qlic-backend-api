package com.wasd.qlic.subscriptions.interfaces.rest

import com.wasd.qlic.subscriptions.application.handlers.*
import com.wasd.qlic.subscriptions.domain.model.*
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

/** The account is taken from the access token, so the request does not carry an accountId. */
data class PurchaseSubscriptionRequest(
    val plan: PlanType,
    val paymentMethod: String = "YAPE_PLIN_OR_CARD",
    val simulatePaymentRejected: Boolean = false
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
@Tag(name = "Subscriptions", description = "Plans, free trial and payments of the account of the session")
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

    @PostMapping
    @Operation(
        summary = "Purchase a subscription plan (US31)",
        description = "Activates a paid plan for 30 days and registers the payment. Answers 201 when the payment is " +
            "confirmed and 402 when it is rejected (use simulatePaymentRejected=true to test it); in that case " +
            "the subscription is not activated. The plans are listed in GET /api/v1/plans."
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun purchaseSubscription(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody req: PurchaseSubscriptionRequest
    ): ResponseEntity<PurchaseSubscriptionResponse> {
        val result = handler.purchaseSubscription(
            PurchaseSubscriptionCommand(
                accountId = jwt.accountId(),
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

    @PostMapping("/trial")
    @Operation(
        summary = "Start the free trial, once per subscriber (US32)",
        description = "Starts the 14-day TRIAL plan, which enables up to 2 Water Points. The trial is granted only once per " +
            "subscriber; a second request returns 409."
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun startFreeTrial(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<Any> {
        return try {
            val sub = handler.startFreeTrial(StartTrialCommand(jwt.accountId()))
            ResponseEntity.status(HttpStatus.CREATED).body(sub.toResource())
        } catch (ex: IllegalStateException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to ex.message))
        }
    }

    @GetMapping("/current")
    @Operation(
        summary = "Get the current subscription of the account and its features (US32)",
        description = "Returns the plan, status, end of the current period and whether the paid features are enabled. A " +
            "trial that ended without a purchased plan is shown as TRIAL_EXPIRED. Returns 404 when the account " +
            "never had a subscription."
    )
    fun getCurrentSubscription(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<SubscriptionResource> {
        val sub = handler.getCurrentSubscription(jwt.accountId())
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sub.toResource())
    }

    @PatchMapping("/{subscriptionId}/plan")
    @Operation(
        summary = "Change the plan from the next billing period (US33)",
        description = "Schedules the new plan in nextBillingPlan; the current plan stays until the end of the paid period. " +
            "Only an ACTIVE subscription can change plan and it cannot change to TRIAL."
    )
    fun changePlan(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable subscriptionId: UUID,
        @RequestBody req: ChangePlanRequest
    ): ResponseEntity<SubscriptionResource> {
        val updated = handler.changePlan(ChangePlanCommand(subscriptionId, jwt.accountId(), req.newPlan))
        return ResponseEntity.ok(updated.toResource())
    }

    @PostMapping("/{subscriptionId}/cancel")
    @Operation(
        summary = "Cancel the subscription keeping access until the end of the paid period (US33)",
        description = "Turns off the automatic renewal: the status becomes CANCELLED_PENDING_EXPIRATION and the features " +
            "stay enabled until currentPeriodEndsAt. No new payments are made."
    )
    fun cancelSubscription(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable subscriptionId: UUID
    ): ResponseEntity<SubscriptionResource> {
        val cancelled = handler.cancelSubscription(CancelSubscriptionCommand(subscriptionId, jwt.accountId()))
        return ResponseEntity.ok(cancelled.toResource())
    }

    @GetMapping("/payments")
    @Operation(
        summary = "List the payment history of the account (US34)",
        description = "Returns every payment attempt of the account with the plan, amount in soles, method, status " +
            "(CONFIRMED or REJECTED) and date."
    )
    fun getPaymentHistory(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<PaymentRecordResource>> {
        val payments = handler.getPaymentHistory(jwt.accountId()).map { it.toResource() }
        return ResponseEntity.ok(payments)
    }
}