package com.wasd.qlic.monitoring.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity: volume of water measured by an IoT device for its Water Point at a moment, with the flow
 * and pressure of the line when the device reports them. The readings feed the monitoring dashboard
 * and the reports of Consumption Analytics.
 */
class ConsumptionReading(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val deviceId: UUID,
    val waterPointId: UUID,
    val volumeLiters: BigDecimal,
    val recordedAt: LocalDateTime = LocalDateTime.now(),
    val flowRateLitersPerMinute: BigDecimal? = null,
    val pressureBar: BigDecimal? = null
) {
    init {
        require(volumeLiters.signum() >= 0) { "The volume cannot be negative" }
        require(flowRateLitersPerMinute == null || flowRateLitersPerMinute.signum() >= 0) { "The flow cannot be negative" }
        require(pressureBar == null || pressureBar.signum() >= 0) { "The pressure cannot be negative" }
    }

    /**
     * Flow of the line in liters per hour: the flow reported by the device or, when it did not report it, the
     * volume of the reading spread over the minutes since the previous reading of the same Water Point.
     */
    fun flowLitersPerHour(previous: ConsumptionReading? = null): BigDecimal? {
        flowRateLitersPerMinute?.let { return it.multiply(MINUTES_PER_HOUR).setScale(2, RoundingMode.HALF_UP) }
        val minutes = previous?.let { Duration.between(it.recordedAt, recordedAt).toMinutes() } ?: return null
        if (minutes <= 0 || minutes > SUSTAINED_WINDOW_MINUTES) return null
        return volumeLiters.multiply(MINUTES_PER_HOUR).divide(BigDecimal.valueOf(minutes), 2, RoundingMode.HALF_UP)
    }

    companion object {
        private val MINUTES_PER_HOUR = BigDecimal("60")

        /** Two readings above the threshold within this window make a sustained flow (US15). */
        const val SUSTAINED_WINDOW_MINUTES = 30L
    }
}

/**
 * Domain Event: a device registered a reading. Alerting evaluates it against the threshold of the Water Point, so a
 * leak is detected as soon as the meter reports it (US15). previousFlowLitersPerHour is the flow of the previous
 * reading of the same Water Point when it was registered within the sustained window; null otherwise.
 */
data class ConsumptionReadingRegisteredEvent(
    val accountId: UUID,
    val deviceId: UUID,
    val waterPointId: UUID,
    val readingId: UUID,
    val recordedAt: LocalDateTime,
    val flowLitersPerHour: BigDecimal?,
    val previousFlowLitersPerHour: BigDecimal?
)
