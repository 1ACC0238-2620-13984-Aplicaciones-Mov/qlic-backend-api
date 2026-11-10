package com.wasd.qlic.monitoring.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The device does not exist in the account of the session. */
class ReadingDeviceNotFoundException : ResourceNotFoundException("monitoring.device.not-found")

/** Readings are recorded per Water Point, so the device must be assigned to one first (US11). */
class DeviceWithoutWaterPointException : BusinessConflictException("monitoring.device.without-water-point")
