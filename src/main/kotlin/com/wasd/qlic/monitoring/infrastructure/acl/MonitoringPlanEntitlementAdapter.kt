package com.wasd.qlic.monitoring.infrastructure.acl

import com.wasd.qlic.monitoring.domain.services.PlanEntitlementProvider
import com.wasd.qlic.subscriptions.domain.repositories.SubscriptionRepository
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Anti-corruption layer with Subscriptions: the plan or trial in force determines how many Water
 * Points the account can monitor. Without access to the paid features no Water Point can be added.
 */
@Component
class MonitoringPlanEntitlementAdapter(
    private val subscriptionRepository: SubscriptionRepository
) : PlanEntitlementProvider {

    override fun maxWaterPoints(accountId: UUID): Int {
        val subscription = subscriptionRepository.findByAccountId(accountId) ?: return 0
        return if (subscription.hasPaidFeaturesAccess()) subscription.currentPlan.maxWaterPoints else 0
    }
}
