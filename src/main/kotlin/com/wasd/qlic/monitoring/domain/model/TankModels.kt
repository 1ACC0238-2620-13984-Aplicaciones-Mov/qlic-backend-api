package com.wasd.qlic.monitoring.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/** Level of the tank derived from its last reading. */
enum class TankStatus { NO_DATA, NORMAL, LOW }

/**
 * Entity: storage tank of a location whose level is monitored by a device (US28). When the level falls
 * below its critical value it raises TankLevelLowEvent once (US29); the alert is rearmed when the tank
 * is refilled above the critical value plus a margin, so a level oscillating around the limit does not
 * notify repeatedly.
 */
class WaterTank(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    locationId: UUID,
    name: String,
    capacityLiters: BigDecimal,
    criticalLevelPercentage: BigDecimal = BigDecimal(20),
    deviceId: UUID? = null,
    currentLevelPercentage: BigDecimal? = null,
    lastReadingAt: LocalDateTime? = null,
    lowLevelAlertActive: Boolean = false,
    val createdAt: Instant = Instant.now()
) {
    var locationId: UUID = locationId
        private set
    var name: String = name.trim()
        private set
    var capacityLiters: BigDecimal = capacityLiters
        private set
    var criticalLevelPercentage: BigDecimal = criticalLevelPercentage
        private set
    var deviceId: UUID? = deviceId
        private set
    var currentLevelPercentage: BigDecimal? = currentLevelPercentage
        private set
    var lastReadingAt: LocalDateTime? = lastReadingAt
        private set
    var lowLevelAlertActive: Boolean = lowLevelAlertActive
        private set

    init {
        validate(this.name, capacityLiters, criticalLevelPercentage)
    }

    val status: TankStatus
        get() = when (val level = currentLevelPercentage) {
            null -> TankStatus.NO_DATA
            else -> if (level <= criticalLevelPercentage) TankStatus.LOW else TankStatus.NORMAL
        }

    /** TankLevel: liters available according to the last reading. */
    val availableLiters: BigDecimal?
        get() = currentLevelPercentage?.let {
            capacityLiters.multiply(it).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        }

    fun update(name: String, locationId: UUID, capacityLiters: BigDecimal, criticalLevelPercentage: BigDecimal, deviceId: UUID?) {
        validate(name.trim(), capacityLiters, criticalLevelPercentage)
        this.name = name.trim()
        this.locationId = locationId
        this.capacityLiters = capacityLiters
        this.criticalLevelPercentage = criticalLevelPercentage
        this.deviceId = deviceId
    }

    /**
     * Registers a level reading. Returns true when the level has just crossed the critical value, which
     * is when TankLevelLowEvent must be published.
     */
    fun registerLevel(levelPercentage: BigDecimal, recordedAt: LocalDateTime): Boolean {
        require(levelPercentage >= BigDecimal.ZERO && levelPercentage <= BigDecimal(100)) { "The level must be between 0 and 100" }
        val previousReadingAt = lastReadingAt
        if (previousReadingAt == null || !recordedAt.isBefore(previousReadingAt)) {
            currentLevelPercentage = levelPercentage
            lastReadingAt = recordedAt
        }
        val level = currentLevelPercentage ?: return false
        return when {
            level <= criticalLevelPercentage && !lowLevelAlertActive -> {
                lowLevelAlertActive = true
                true
            }
            level > criticalLevelPercentage + REARM_MARGIN -> {
                lowLevelAlertActive = false
                false
            }
            else -> false
        }
    }

    private fun validate(name: String, capacity: BigDecimal, critical: BigDecimal) {
        require(name.isNotBlank()) { "The name of the tank is required" }
        require(capacity.signum() > 0) { "The capacity must be positive" }
        require(critical > BigDecimal.ZERO && critical < BigDecimal(100)) { "The critical level must be between 0 and 100" }
    }

    companion object {
        val REARM_MARGIN = BigDecimal(5)
    }
}

/** Level reported by the device of a tank at a moment. */
data class TankLevelReading(
    val id: UUID = UUID.randomUUID(),
    val tankId: UUID,
    val accountId: UUID,
    val levelPercentage: BigDecimal,
    val recordedAt: LocalDateTime
)

/** Value Object: moment in which a tank is expected to reach its critical level (US30). */
data class ReplenishmentPrediction(
    val available: Boolean,
    val predictedCriticalAt: LocalDateTime? = null,
    val hoursToCritical: BigDecimal? = null,
    val consumptionPercentagePerHour: BigDecimal? = null
) {
    companion object {
        val UNAVAILABLE = ReplenishmentPrediction(available = false)
    }
}

/** Domain Event: the level of a tank fell below its critical value (US29). */
data class TankLevelLowEvent(
    val accountId: UUID,
    val tankId: UUID,
    val tankName: String,
    val levelPercentage: BigDecimal,
    val criticalLevelPercentage: BigDecimal,
    val occurredAt: LocalDateTime
)
