package com.wasd.qlic.monitoring.application.commands

import java.util.UUID

/** US10: registers an IoT device in the account of the session by reading its QR code. */
data class RegisterDeviceCommand(
    val qrCodePayload: String,
    val serialNumber: String,
    val model: String,
    val accountId: UUID
)

/** US11: assigns a device of the account of the session to a Water Point. */
data class AssignWaterPointCommand(
    val deviceId: UUID,
    val waterPointId: UUID,
    val accountId: UUID
)

/**
 * TS02: updates the editable data of a device of the account of the session. A null waterPointId
 * unassigns the device; a blank name removes it.
 */
data class UpdateDeviceCommand(
    val deviceId: UUID,
    val accountId: UUID,
    val name: String?,
    val waterPointId: UUID?
)
