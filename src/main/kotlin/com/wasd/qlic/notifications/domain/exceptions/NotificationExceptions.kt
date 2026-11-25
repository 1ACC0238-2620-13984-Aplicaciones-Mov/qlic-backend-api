package com.wasd.qlic.notifications.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The device token is not registered in the account of the session. */
class DeviceTokenNotFoundException : ResourceNotFoundException("notifications.device-token.not-found")
