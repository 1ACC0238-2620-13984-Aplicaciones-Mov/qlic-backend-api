package com.wasd.qlic.consumption.application.handlers

import com.wasd.qlic.consumption.application.commands.DefineActivityScheduleCommand
import com.wasd.qlic.consumption.application.queries.GetActivityConsumptionReportQuery
import com.wasd.qlic.consumption.application.queries.GetActivityScheduleQuery
import com.wasd.qlic.consumption.domain.exceptions.ActivityScheduleNotDefinedException
import com.wasd.qlic.consumption.domain.exceptions.InvalidActivityScheduleException
import com.wasd.qlic.consumption.domain.exceptions.InvalidPeriodException
import com.wasd.qlic.consumption.domain.model.ActivityConsumptionReport
import com.wasd.qlic.consumption.domain.model.ActivitySchedule
import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.PremisesActivitySchedule
import com.wasd.qlic.consumption.domain.repositories.ActivityScheduleRepository
import com.wasd.qlic.consumption.domain.services.ConsumptionAggregationService
import com.wasd.qlic.consumption.domain.services.ConsumptionReadingProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** US24: validates and stores the activity schedule of the premises. */
@Component
class DefineActivityScheduleCommandHandler(
    private val scheduleRepository: ActivityScheduleRepository
) {
    @Transactional
    fun handle(command: DefineActivityScheduleCommand): PremisesActivitySchedule {
        val schedule = try {
            ActivitySchedule(command.openingTime, command.closingTime, command.activeDays)
        } catch (exception: IllegalArgumentException) {
            throw InvalidActivityScheduleException()
        }
        val current = scheduleRepository.findByAccountId(command.accountId)
        val updated = current?.also { it.redefine(schedule) }
            ?: PremisesActivitySchedule(accountId = command.accountId, schedule = schedule)
        return scheduleRepository.save(updated)
    }
}

/**
 * US24 - Scenario 1: retrieves the readings of the period and the declared schedule and invokes the
 * aggregation, which differentiates the consumption inside and outside the activity hours.
 */
@Component
@Transactional(readOnly = true)
class ActivityConsumptionQueryHandler(
    private val scheduleRepository: ActivityScheduleRepository,
    private val readingProvider: ConsumptionReadingProvider,
    private val aggregationService: ConsumptionAggregationService
) {
    fun handle(query: GetActivityConsumptionReportQuery): ActivityConsumptionReport {
        val period = try {
            Period(query.from, query.to)
        } catch (exception: IllegalArgumentException) {
            throw InvalidPeriodException()
        }
        val schedule = scheduleRepository.findByAccountId(query.accountId)?.schedule
        val readings = readingProvider.readings(query.accountId, period)
        return aggregationService.aggregateByActivity(query.accountId, period, readings, schedule)
    }

    fun handle(query: GetActivityScheduleQuery): PremisesActivitySchedule =
        scheduleRepository.findByAccountId(query.accountId) ?: throw ActivityScheduleNotDefinedException()
}
