package com.wasd.qlic.monitoring.domain.services

import java.util.UUID

/** Domain port: limits of the current plan, provided by the Subscriptions bounded context. */
interface PlanEntitlementProvider {
    /** Number of Water Points the account can monitor; 0 without an active plan or trial. */
    fun maxWaterPoints(accountId: UUID): Int
}
