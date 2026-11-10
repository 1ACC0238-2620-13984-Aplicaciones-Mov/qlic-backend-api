package com.wasd.qlic.iam.domain.services

import java.util.UUID

/**
 * Domain port: maximum number of additional users allowed by the current plan of an account (US08).
 * Provided by the Subscriptions bounded context.
 */
interface PlanEntitlementProvider {
    fun maxAuthorizedUsers(accountId: UUID): Int
}
