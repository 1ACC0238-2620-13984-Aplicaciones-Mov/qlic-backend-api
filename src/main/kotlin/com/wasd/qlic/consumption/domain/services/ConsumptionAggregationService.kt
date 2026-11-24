package com.wasd.qlic.consumption.domain.services

import com.wasd.qlic.consumption.domain.model.ActivityConsumptionReport
import com.wasd.qlic.consumption.domain.model.ActivitySchedule
import com.wasd.qlic.consumption.domain.model.Period
import com.wasd.qlic.consumption.domain.model.ReadingSnapshot
import com.wasd.qlic.consumption.domain.model.WaterPointActivityConsumption
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Domain Service: aggregates the readings of the period by Water Point and, when the premises declared
 * its ActivitySchedule, separates the volume consumed inside and outside the activity hours (US24).
 */
@Service
class ConsumptionAggregationService {

    fun aggregateByActivity(
        accountId: UUID,
        period: Period,
        readings: List<ReadingSnapshot>,
        schedule: ActivitySchedule?
    ): ActivityConsumptionReport {
        val byWaterPoint = readings.groupBy { it.waterPointId }.map { (waterPointId, items) ->
            val total = items.sumOf { it.volumeLiters }
            val within = schedule?.let { s -> items.filter { s.isWithinActivity(it.recordedAt) }.sumOf { it.volumeLiters } }
            WaterPointActivityConsumption(
                waterPointId = waterPointId,
                totalLiters = total.scaled(),
                withinActivityLiters = within?.scaled(),
                outsideActivityLiters = within?.let { (total - it).scaled() }
            )
        }.sortedByDescending { it.outsideActivityLiters ?: it.totalLiters }

        val total = readings.sumOf { it.volumeLiters }
        val within = schedule?.let { byWaterPoint.sumOf { it.withinActivityLiters ?: BigDecimal.ZERO } }
        val outside = within?.let { total - it }
        val outsidePercentage = outside?.let {
            if (total.signum() == 0) BigDecimal.ZERO.setScale(2)
            else it.multiply(BigDecimal(100)).divide(total, 2, RoundingMode.HALF_UP)
        }

        return ActivityConsumptionReport(
            accountId = accountId,
            period = period,
            schedule = schedule,
            totalLiters = total.scaled(),
            withinActivityLiters = within?.scaled(),
            outsideActivityLiters = outside?.scaled(),
            outsideActivityPercentage = outsidePercentage,
            byWaterPoint = byWaterPoint
        )
    }

    private fun BigDecimal.scaled(): BigDecimal = setScale(2, RoundingMode.HALF_UP)
}
