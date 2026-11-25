package com.wasd.qlic.monitoring.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.DomainException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The tank does not exist or belongs to another account (US28). */
class WaterTankNotFoundException : ResourceNotFoundException("monitoring.tank.not-found")

/** Name, capacity, critical level or reported level out of range. */
class InvalidWaterTankException : DomainException("monitoring.tank.invalid")
