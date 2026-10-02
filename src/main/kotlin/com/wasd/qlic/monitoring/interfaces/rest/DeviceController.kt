package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.application.handlers.DeviceCommandHandler
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import com.wasd.qlic.monitoring.interfaces.rest.resources.AssignWaterPointRequest
import com.wasd.qlic.monitoring.interfaces.rest.resources.IoTDeviceResource
import com.wasd.qlic.monitoring.interfaces.rest.resources.RegisterDeviceRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/devices")
class DeviceController(
    private val deviceCommandHandler: DeviceCommandHandler
) {

    // US10: Registrar dispositivo IoT mediante QR[cite: 5]
    @PostMapping
    fun registerDevice(@RequestBody request: RegisterDeviceRequest): ResponseEntity<IoTDeviceResource> {
        val command = RegisterDeviceCommand(
            qrCodePayload = request.qrCodePayload,
            serialNumber = request.serialNumber,
            model = request.model,
            accountId = request.accountId
        )
        val device = deviceCommandHandler.handleRegister(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            IoTDeviceResource(
                id = device.id,
                serialNumber = device.serialNumber,
                model = device.model,
                accountId = device.accountId,
                waterPointId = device.waterPointId,
                status = device.status,
                batteryPercentage = device.batteryPercentage,
                lastSyncAt = device.lastSyncAt
            )
        )
    }

    // US11: Asignar un dispositivo a un Water Point[cite: 5]
    @PatchMapping("/{id}/water-point")
    fun assignWaterPoint(
        @PathVariable id: UUID,
        @RequestBody request: AssignWaterPointRequest
    ): ResponseEntity<IoTDeviceResource> {
        val command = AssignWaterPointCommand(deviceId = id, waterPointId = request.waterPointId)
        val updated = deviceCommandHandler.handleAssignWaterPoint(command)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            IoTDeviceResource(
                id = updated.id,
                serialNumber = updated.serialNumber,
                model = updated.model,
                accountId = updated.accountId,
                waterPointId = updated.waterPointId,
                status = updated.status,
                batteryPercentage = updated.batteryPercentage,
                lastSyncAt = updated.lastSyncAt
            )
        )
    }

    // US12: Consultar estado de los dispositivos registrados[cite: 5]
    @GetMapping
    fun getDevicesByAccount(@RequestParam accountId: UUID): ResponseEntity<List<IoTDeviceResource>> {
        val query = GetDevicesByAccountQuery(accountId)
        val devices = deviceCommandHandler.handleGetByAccount(query).map {
            IoTDeviceResource(
                id = it.id,
                serialNumber = it.serialNumber,
                model = it.model,
                accountId = it.accountId,
                waterPointId = it.waterPointId,
                status = it.status,
                batteryPercentage = it.batteryPercentage,
                lastSyncAt = it.lastSyncAt
            )
        }
        return ResponseEntity.ok(devices)
    }
}