package com.wasd.qlic.monitoring.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.DomainException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** The location does not exist or belongs to another account (US13). */
class LocationNotFoundException : ResourceNotFoundException("monitoring.location.not-found")

/** A location with Water Points cannot be deleted; they must be moved or deleted first. */
class LocationHasWaterPointsException : BusinessConflictException("monitoring.location.has-water-points")

/** The Water Point does not exist or belongs to another account. */
class WaterPointNotFoundException : ResourceNotFoundException("monitoring.water-point.not-found")

/** A Water Point with assigned devices cannot be deleted. */
class WaterPointHasDevicesException : BusinessConflictException("monitoring.water-point.has-devices")

/** The current plan does not allow more Water Points (PlanType.maxWaterPoints). */
class WaterPointLimitReachedException(limit: Int) :
    BusinessConflictException("monitoring.water-point.limit-reached", limit)

/** The name is blank or too long. */
class InvalidLocationDataException : DomainException("monitoring.location.invalid")

/** A location with tanks cannot be deleted; they must be deleted first. */
class LocationHasTanksException : BusinessConflictException("monitoring.location.has-tanks")
