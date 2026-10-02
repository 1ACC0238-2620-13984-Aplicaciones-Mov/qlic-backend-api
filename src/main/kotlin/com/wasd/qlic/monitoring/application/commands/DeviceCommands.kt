package com.wasd.qlic.monitoring.application.commands

import java.util.UUID

// US10: Registrar mediante QR
data class RegisterDeviceCommand(
    val qrCodePayload: String,
    val serialNumber: String,
    val model: String,
    val accountId: UUID
)

// US11: Asignar a un Water Point
data class AssignWaterPointCommand(
    val deviceId: UUID,
    val waterPointId: UUID
)