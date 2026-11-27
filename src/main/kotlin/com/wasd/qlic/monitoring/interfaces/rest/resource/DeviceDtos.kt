package com.wasd.qlic.monitoring.interfaces.rest.resources

import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.SimulationScenario
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

/** The account is taken from the access token, so the request does not carry an accountId. */
data class RegisterDeviceRequest(
    val qrCodePayload: String,
    val serialNumber: String,
    val model: String
)

/** TS02: full replacement of the editable data of a device. */
data class UpdateDeviceRequest(
    @field:Schema(description = "Friendly name shown in the apps; null or blank removes it", example = "Medidor cocina", nullable = true)
    val name: String? = null,
    @field:Schema(description = "Water Point of the account to assign; null unassigns the device", nullable = true)
    val waterPointId: UUID? = null
)

data class AssignWaterPointRequest(
    val waterPointId: UUID
)

data class IoTDeviceResource(
    val id: UUID,
    val serialNumber: String,
    val model: String,
    val accountId: UUID,
    @field:Schema(description = "Friendly name; null when the device has none", example = "Medidor cocina", nullable = true)
    val name: String?,
    val waterPointId: UUID?,
    val status: DeviceStatus,
    val batteryPercentage: Int,
    val lastSyncAt: LocalDateTime,
    @field:Schema(description = "Scenario of the simulation mode; null when the device sends real readings", example = "LEAK", nullable = true)
    val simulationScenario: SimulationScenario?
)

/** Simulation mode of a device, for demonstrations and user validation without a physical meter. */
data class DeviceSimulationRequest(
    @field:Schema(description = "true starts sending simulated readings; false stops them", example = "true")
    val enabled: Boolean,
    @field:Schema(description = "NORMAL follows a daily usage profile; LEAK adds a continuous flow above the threshold", example = "LEAK")
    val scenario: SimulationScenario = SimulationScenario.NORMAL
)

fun IoTDevice.toResource() = IoTDeviceResource(
    id = id,
    serialNumber = serialNumber,
    model = model,
    accountId = accountId,
    name = name,
    waterPointId = waterPointId,
    status = effectiveStatus(),
    batteryPercentage = batteryPercentage,
    lastSyncAt = lastSyncAt,
    simulationScenario = simulationScenario
)
