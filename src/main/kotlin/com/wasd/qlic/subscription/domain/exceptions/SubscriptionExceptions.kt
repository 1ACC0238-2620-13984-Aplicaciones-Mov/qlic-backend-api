package com.wasd.qlic.subscriptions.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The subscription does not exist or belongs to another account (HTTP 404, the id is not disclosed). */
class SubscriptionNotFoundException : ResourceNotFoundException("subscriptions.subscription.not-found")

/** US32: the free trial is granted only once per subscriber (HTTP 409). */
class TrialAlreadyUsedException : BusinessConflictException("subscriptions.trial.already-used")
