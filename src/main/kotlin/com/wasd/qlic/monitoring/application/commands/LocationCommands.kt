package com.wasd.qlic.monitoring.application.commands

import com.wasd.qlic.monitoring.domain.model.WaterPointType
import java.util.UUID

/** US13: registers a premises or home of the account. */
data class CreateLocationCommand(val accountId: UUID, val name: String, val address: String?)

data class UpdateLocationCommand(val locationId: UUID, val accountId: UUID, val name: String, val address: String?)

data class DeleteLocationCommand(val locationId: UUID, val accountId: UUID)

/** US13: registers a Water Point inside a location of the account, within the limit of the plan. */
data class CreateWaterPointCommand(
    val accountId: UUID,
    val locationId: UUID,
    val name: String,
    val type: WaterPointType
)

data class UpdateWaterPointCommand(
    val waterPointId: UUID,
    val accountId: UUID,
    val locationId: UUID,
    val name: String,
    val type: WaterPointType
)

data class DeleteWaterPointCommand(val waterPointId: UUID, val accountId: UUID)

/** Unlinks a device from the account. */
data class DeleteDeviceCommand(val deviceId: UUID, val accountId: UUID)
