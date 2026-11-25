package com.wasd.qlic.monitoring.domain.model

import java.time.LocalDateTime
import java.util.UUID

enum class DeviceStatus {
    ONLINE,
    OFFLINE,
    LOW_BATTERY,
    CALIBRATING
}

class IoTDevice(
    val id: UUID = UUID.randomUUID(),
    val serialNumber: String,
    val model: String,
    val qrCodePayload: String,
    val accountId: UUID,
    var waterPointId: UUID? = null,
    var status: DeviceStatus = DeviceStatus.ONLINE,
    var batteryPercentage: Int = 100,
    var lastSyncAt: LocalDateTime = LocalDateTime.now()
) {
    fun assignWaterPoint(waterPointId: UUID) {
        this.waterPointId = waterPointId
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
    }
}
