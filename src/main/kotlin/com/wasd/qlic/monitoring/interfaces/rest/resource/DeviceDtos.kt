package com.wasd.qlic.monitoring.interfaces.rest.resources

import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import java.time.LocalDateTime
import java.util.UUID

/** The account is taken from the access token, so the request does not carry an accountId. */
data class RegisterDeviceRequest(
    val qrCodePayload: String,
    val serialNumber: String,
    val model: String
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

fun IoTDevice.toResource() = IoTDeviceResource(
    id = id,
    serialNumber = serialNumber,
    model = model,
    accountId = accountId,
    waterPointId = waterPointId,
    status = status,
    batteryPercentage = batteryPercentage,
    lastSyncAt = lastSyncAt
)
