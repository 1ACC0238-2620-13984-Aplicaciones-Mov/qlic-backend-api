package com.wasd.qlic.monitoring.interfaces.rest.resources

import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import java.time.LocalDateTime
import java.util.UUID

data class RegisterDeviceRequest(
    val qrCodePayload: String,
    val serialNumber: String,
    val model: String,
    val accountId: UUID
)

data class AssignWaterPointRequest(
    val waterPointId: UUID
)

data class IoTDeviceResource(
    val id: UUID,
    val serialNumber: String,
    val model: String,
    val accountId: UUID,
    val waterPointId: UUID?,
    val status: DeviceStatus,
    val batteryPercentage: Int,
    val lastSyncAt: LocalDateTime
)