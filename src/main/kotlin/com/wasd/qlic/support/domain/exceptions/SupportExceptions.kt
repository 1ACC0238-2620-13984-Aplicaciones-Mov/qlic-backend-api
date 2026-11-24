package com.wasd.qlic.support.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.DomainException
import com.wasd.qlic.shared.domain.exceptions.OperationNotPermittedException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** US35 - Scenario 1: the subscriber must select the affected device, Water Point or alert. */
class IncidentAssetRequiredException : DomainException("support.incident.asset-required")

class IncidentNotFoundException : ResourceNotFoundException("support.incident.not-found")

/** US35: the device does not belong to the account of the session. */
class DeviceNotFoundForAccountException : ResourceNotFoundException("support.device.not-found")

/** US35 - Scenario 2: the alert does not exist in the account of the session. */
class AlertNotFoundForAccountException : ResourceNotFoundException("support.alert.not-found")

/** US35 - Scenario 2: incidents can only be derived from an active alert. */
class AlertNotActiveException : BusinessConflictException("support.alert.not-active")

/** US36 / US37: the requested state change is not valid for the current state of the incident. */
class InvalidIncidentTransitionException(from: String, to: String) :
    BusinessConflictException("support.incident.invalid-transition", from, to)

/** US36: the current plan does not cover technical visits. */
class TechnicalVisitNotCoveredException : OperationNotPermittedException("support.visit.not-covered")

class TechnicianNotFoundException : ResourceNotFoundException("support.technician.not-found")

class NoTechnicianAvailableException : BusinessConflictException("support.technician.unavailable")

class TermsOfServiceNotFoundException : ResourceNotFoundException("support.terms.not-found")
