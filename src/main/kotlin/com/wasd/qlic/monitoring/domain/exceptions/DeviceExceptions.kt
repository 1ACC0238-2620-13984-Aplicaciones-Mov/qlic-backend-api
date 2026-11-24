package com.wasd.qlic.monitoring.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The device does not exist or belongs to another account (HTTP 404, the id is not disclosed). */
class DeviceNotFoundException : ResourceNotFoundException("monitoring.device.not-found")

/** The serial number read from the QR code is already registered in another account (US10). */
class DeviceAlreadyRegisteredException(serialNumber: String) :
    BusinessConflictException("monitoring.device.already-registered", serialNumber)
