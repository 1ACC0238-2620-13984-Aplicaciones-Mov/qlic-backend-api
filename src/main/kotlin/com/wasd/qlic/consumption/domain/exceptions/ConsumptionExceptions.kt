package com.wasd.qlic.consumption.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.DomainException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** US24: the schedule has not been declared yet for the premises. */
class ActivityScheduleNotDefinedException : ResourceNotFoundException("consumption.schedule.not-defined")

/** US24: opening and closing times are equal or no active day was selected. */
class InvalidActivityScheduleException : DomainException("consumption.schedule.invalid")

/** The requested period is not valid. */
class InvalidPeriodException : DomainException("consumption.period.invalid")
