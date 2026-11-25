package com.wasd.qlic.monitoring.domain.services

import com.wasd.qlic.monitoring.domain.model.ReplenishmentPrediction
import com.wasd.qlic.monitoring.domain.model.TankLevelReading
import com.wasd.qlic.monitoring.domain.model.WaterTank
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration

/**
 * ReplenishmentPredictionService (US30): estimates when the tank will reach its critical level from the
 * consumption pattern since the last refill. The rate is the least-squares slope of the level over time;
 * it needs at least 3 readings spanning 12 hours with a falling level (Scenario 2 otherwise).
 */
@Service
class ReplenishmentPredictionService {

    fun predict(tank: WaterTank, history: List<TankLevelReading>): ReplenishmentPrediction {
        val current = tank.currentLevelPercentage ?: return ReplenishmentPrediction.UNAVAILABLE
        val lastReadingAt = tank.lastReadingAt ?: return ReplenishmentPrediction.UNAVAILABLE
        val sinceRefill = sinceLastRefill(history.sortedBy { it.recordedAt })
        if (sinceRefill.size < MIN_READINGS) return ReplenishmentPrediction.UNAVAILABLE

        val start = sinceRefill.first().recordedAt
        val spanHours = Duration.between(start, sinceRefill.last().recordedAt).toMinutes() / 60.0
        if (spanHours < MIN_SPAN_HOURS) return ReplenishmentPrediction.UNAVAILABLE

        val xs = sinceRefill.map { Duration.between(start, it.recordedAt).toMinutes() / 60.0 }
        val ys = sinceRefill.map { it.levelPercentage.toDouble() }
        val meanX = xs.average()
        val meanY = ys.average()
        val denominator = xs.sumOf { (it - meanX) * (it - meanX) }
        if (denominator == 0.0) return ReplenishmentPrediction.UNAVAILABLE
        val slope = xs.indices.sumOf { (xs[it] - meanX) * (ys[it] - meanY) } / denominator
        if (slope >= 0.0) return ReplenishmentPrediction.UNAVAILABLE

        val ratePerHour = -slope
        val remaining = (current - tank.criticalLevelPercentage).toDouble()
        val hours = if (remaining <= 0.0) 0.0 else remaining / ratePerHour
        return ReplenishmentPrediction(
            available = true,
            predictedCriticalAt = lastReadingAt.plusMinutes((hours * 60).toLong()),
            hoursToCritical = BigDecimal(hours).setScale(1, RoundingMode.HALF_UP),
            consumptionPercentagePerHour = BigDecimal(ratePerHour).setScale(2, RoundingMode.HALF_UP)
        )
    }

    /** Readings after the last refill: a rise of more than 5 points restarts the consumption pattern. */
    private fun sinceLastRefill(readings: List<TankLevelReading>): List<TankLevelReading> {
        var startIndex = 0
        for (index in 1 until readings.size) {
            if (readings[index].levelPercentage - readings[index - 1].levelPercentage > REFILL_THRESHOLD) startIndex = index
        }
        return readings.subList(startIndex, readings.size)
    }

    private companion object {
        const val MIN_READINGS = 3
        const val MIN_SPAN_HOURS = 12.0
        val REFILL_THRESHOLD = BigDecimal(5)
    }
}
