package com.wasd.qlic.subscriptions.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The subscription does not exist or belongs to another account (HTTP 404, the id is not disclosed). */
class SubscriptionNotFoundException : ResourceNotFoundException("subscriptions.subscription.not-found")
