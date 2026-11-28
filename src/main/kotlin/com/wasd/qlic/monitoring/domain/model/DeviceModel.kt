package com.wasd.qlic.monitoring.domain.model

import java.time.LocalDateTime
import java.util.UUID

enum class DeviceStatus {
    ONLINE,
    OFFLINE,
    LOW_BATTERY,
    CALIBRATING
}

/**
 * Behaviour of a device in simulation mode, used for demonstrations and user validation without a physical meter:
 * NORMAL follows a daily usage profile and LEAK adds a continuous flow above any usual threshold.
 */
enum class SimulationScenario {
    NORMAL,
    LEAK
}

class IoTDevice(
    val id: UUID = UUID.randomUUID(),
    val serialNumber: String,
    val model: String,
    val qrCodePayload: String,
    val accountId: UUID,
    var name: String? = null,
    var waterPointId: UUID? = null,
    var status: DeviceStatus = DeviceStatus.ONLINE,
    var batteryPercentage: Int = 100,
    var lastSyncAt: LocalDateTime = LocalDateTime.now(),
    var simulationScenario: SimulationScenario? = null
) {
    val simulated: Boolean get() = simulationScenario != null

    /** The device starts sending simulated readings with the given scenario. */
    fun startSimulation(scenario: SimulationScenario) {
        this.simulationScenario = scenario
    }

    /** The device stops sending simulated readings. */
    fun stopSimulation() {
        this.simulationScenario = null
    }

    /**
     * DeviceHealthService rule: a device that has not reported within the limit becomes OFFLINE.
     * Returns true only on the transition, so the disconnection is notified once.
     */
    fun markDisconnectedIfSilent(now: LocalDateTime = LocalDateTime.now()): Boolean {
        if (status == DeviceStatus.OFFLINE || !lastSyncAt.isBefore(now.minusMinutes(DISCONNECTION_LIMIT_MINUTES))) return false
        status = DeviceStatus.OFFLINE
        return true
    }

    fun assignWaterPoint(waterPointId: UUID) {
        this.waterPointId = waterPointId
    }

    /** TS02: the device stops measuring any Water Point until it is assigned again. */
    fun unassignWaterPoint() {
        this.waterPointId = null
    }

    /** TS02: friendly name shown in the apps; blank removes it and the serial number is shown instead. */
    fun rename(newName: String?) {
        val trimmed = newName?.trim()?.takeIf { it.isNotEmpty() }
        require(trimmed == null || trimmed.length <= MAX_NAME_LENGTH) { "The device name cannot exceed $MAX_NAME_LENGTH characters" }
        this.name = trimmed
    }

    /**
     * A reading proves that the device is connected: the synchronization time is refreshed and, when the
     * device reports it, the battery level too.
     */
    fun registerReading(battery: Int?, at: LocalDateTime = LocalDateTime.now()) {
        battery?.let {
            require(it in 0..100) { "The battery percentage must be between 0 and 100" }
            batteryPercentage = it
        }
        lastSyncAt = at
        status = if (batteryPercentage <= LOW_BATTERY_THRESHOLD) DeviceStatus.LOW_BATTERY else DeviceStatus.ONLINE
    }

    /** DeviceHealthService rule: a device that has not reported within the limit is disconnected. */
    fun effectiveStatus(now: LocalDateTime = LocalDateTime.now()): DeviceStatus =
        if (lastSyncAt.isBefore(now.minusMinutes(DISCONNECTION_LIMIT_MINUTES))) DeviceStatus.OFFLINE else status

    fun updateTelemetry(battery: Int, online: Boolean) {
        this.batteryPercentage = battery
        this.lastSyncAt = LocalDateTime.now()
        this.status = when {
            !online -> DeviceStatus.OFFLINE
            battery <= 15 -> DeviceStatus.LOW_BATTERY
            else -> DeviceStatus.ONLINE
        }
    }

    companion object {
        const val LOW_BATTERY_THRESHOLD = 15
        const val DISCONNECTION_LIMIT_MINUTES = 60L
        const val MAX_NAME_LENGTH = 60
    }
}

/**
 * Domain Event: a device stopped reporting for longer than the disconnection limit (DeviceHealthService). Notifications
 * warns the account, because a silent meter also means that a leak would go unnoticed.
 */
data class DeviceDisconnectedEvent(
    val accountId: UUID,
    val deviceId: UUID,
    val deviceName: String,
    val lastSyncAt: LocalDateTime
)
