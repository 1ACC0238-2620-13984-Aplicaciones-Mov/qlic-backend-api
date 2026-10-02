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

    fun updateTelemetry(battery: Int, online: Boolean) {
        this.batteryPercentage = battery
        this.lastSyncAt = LocalDateTime.now()
        this.status = when {
            !online -> DeviceStatus.OFFLINE
            battery <= 15 -> DeviceStatus.LOW_BATTERY
            else -> DeviceStatus.ONLINE
        }
    }
}