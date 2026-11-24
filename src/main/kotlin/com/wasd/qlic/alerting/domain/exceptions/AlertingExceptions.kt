package com.wasd.qlic.alerting.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The alert does not exist or belongs to another account (HTTP 404, the id is not disclosed). */
class AlertNotFoundException : ResourceNotFoundException("alerting.alert.not-found")

/** The authorized recipient does not exist or belongs to another account (US19). */
class AlertRecipientNotFoundException : ResourceNotFoundException("alerting.recipient.not-found")
