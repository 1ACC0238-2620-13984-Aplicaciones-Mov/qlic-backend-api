package com.wasd.qlic.consumption.domain.exceptions

import com.wasd.qlic.shared.domain.exceptions.BusinessConflictException
import com.wasd.qlic.shared.domain.exceptions.DomainException
import com.wasd.qlic.shared.domain.exceptions.ResourceNotFoundException

/** US24: the schedule has not been declared yet for the premises. */
class ActivityScheduleNotDefinedException : ResourceNotFoundException("consumption.schedule.not-defined")

/** US24: opening and closing times are equal or no active day was selected. */
class InvalidActivityScheduleException : DomainException("consumption.schedule.invalid")

/** The requested period is not valid. */
class InvalidPeriodException : DomainException("consumption.period.invalid")

/** US27: the previous month has no readings, so the reduction cannot be measured. */
class GoalBaselineUnavailableException : BusinessConflictException("consumption.goal.baseline-unavailable")

/** US27: the reduction must be greater than 0 % and lower than 100 %. */
class InvalidGoalException : DomainException("consumption.goal.invalid")

/** US27: no goal was defined for the month. */
class SustainabilityGoalNotFoundException : ResourceNotFoundException("consumption.goal.not-found")

/** US23: the custom price per cubic meter must be positive. */
class InvalidTariffException : DomainException("consumption.tariff.invalid")
