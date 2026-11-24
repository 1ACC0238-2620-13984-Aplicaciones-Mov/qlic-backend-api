package com.wasd.qlic.iam.infrastructure.acl

import com.wasd.qlic.iam.domain.services.PlanEntitlementProvider
import com.wasd.qlic.subscriptions.domain.model.PlanType
import com.wasd.qlic.subscriptions.domain.repositories.SubscriptionRepository
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Anti-corruption layer: translates the current plan of the Subscriptions bounded context into the
 * number of additional users allowed by IAM (US08 - Business rule). Without an active plan or
 * trial, the account cannot share its access.
 */
@Component
class PlanEntitlementAdapter(
    private val subscriptionRepository: SubscriptionRepository
) : PlanEntitlementProvider {

    override fun maxAuthorizedUsers(accountId: UUID): Int {
        val subscription = subscriptionRepository.findByAccountId(accountId) ?: return 0
        if (!subscription.hasPaidFeaturesAccess()) return 0
        return when (subscription.currentPlan) {
            PlanType.TRIAL -> 1
            PlanType.HOME_BASIC -> 1
            PlanType.HOME_PRO -> 3
            PlanType.PYME_BUSINESS -> 5
            PlanType.PYME_ENTERPRISE -> 10
        }
    }
}
