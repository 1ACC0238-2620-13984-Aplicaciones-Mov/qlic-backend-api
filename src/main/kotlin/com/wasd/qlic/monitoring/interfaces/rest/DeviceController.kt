package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.AssignWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.DeleteDeviceCommand
import com.wasd.qlic.monitoring.application.commands.RegisterDeviceCommand
import com.wasd.qlic.monitoring.application.commands.UpdateDeviceCommand
import com.wasd.qlic.monitoring.application.handlers.DeviceCommandHandler
import com.wasd.qlic.monitoring.application.queries.GetDeviceByIdQuery
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import com.wasd.qlic.monitoring.interfaces.rest.resources.AssignWaterPointRequest
import com.wasd.qlic.monitoring.interfaces.rest.resources.IoTDeviceResource
import com.wasd.qlic.monitoring.interfaces.rest.resources.RegisterDeviceRequest
import com.wasd.qlic.monitoring.interfaces.rest.resources.UpdateDeviceRequest
import com.wasd.qlic.monitoring.interfaces.rest.resources.toResource
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

/** Device Monitoring endpoints. Every operation works on the account of the access token. */
@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "IoT devices of the account of the session")
class DeviceController(
    private val deviceCommandHandler: DeviceCommandHandler
) {

    @PostMapping
    @Operation(
        summary = "Register an IoT device from its QR code (US10)",
        description = "Registers the device read from its QR code in the account of the session, with status ONLINE and " +
            "100% battery. Registering the same serial number again in the same account returns the existing " +
            "device; a serial number registered in another account returns 409."
    )
    fun registerDevice(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: RegisterDeviceRequest
    ): ResponseEntity<IoTDeviceResource> {
        val device = deviceCommandHandler.handleRegister(
            RegisterDeviceCommand(
                qrCodePayload = request.qrCodePayload,
                serialNumber = request.serialNumber,
                model = request.model,
                accountId = jwt.accountId()
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(device.toResource())
    }

    @PatchMapping("/{id}/water-point")
    @Operation(
        summary = "Assign a device to a Water Point (US11)",
        description = "Links the device to one of the Water Points of the account, so its readings are counted for that " +
            "Water Point. A device or Water Point of another account returns 404."
    )
    fun assignWaterPoint(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: AssignWaterPointRequest
    ): ResponseEntity<IoTDeviceResource> {
        val updated = deviceCommandHandler.handleAssignWaterPoint(
            AssignWaterPointCommand(deviceId = id, waterPointId = request.waterPointId, accountId = jwt.accountId())
        )
        return ResponseEntity.ok(updated.toResource())
    }

    @GetMapping
    @Operation(
        summary = "List the devices of the account and their status (US12)",
        description = "With locationId only the devices assigned to the Water Points of that location are returned (US13)."
    )
    fun getDevicesByAccount(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) locationId: UUID?
    ): ResponseEntity<List<IoTDeviceResource>> =
        ResponseEntity.ok(
            deviceCommandHandler.handleGetByAccount(GetDevicesByAccountQuery(jwt.accountId(), locationId)).map { it.toResource() }
        )

    @GetMapping("/{id}")
    @Operation(
        summary = "Get a device of the account and its status (US12)",
        description = "Returns the device with its effective status: OFFLINE when it has not reported for 60 minutes, " +
            "LOW_BATTERY at 15% or less, otherwise ONLINE."
    )
    fun getDevice(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID
    ): ResponseEntity<IoTDeviceResource> =
        ResponseEntity.ok(deviceCommandHandler.handleGetById(GetDeviceByIdQuery(id, jwt.accountId())).toResource())

    @PutMapping("/{id}")
    @Operation(
        summary = "Update the name and the Water Point of a device (TS02)",
        description = "Full replacement of the editable data: name (null or blank removes it, up to 60 characters) and " +
            "waterPointId (null unassigns the device). The serial number and the model cannot change."
    )
    fun updateDevice(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @RequestBody request: UpdateDeviceRequest
    ): ResponseEntity<IoTDeviceResource> =
        ResponseEntity.ok(
            deviceCommandHandler.handleUpdate(
                UpdateDeviceCommand(deviceId = id, accountId = jwt.accountId(), name = request.name, waterPointId = request.waterPointId)
            ).toResource()
        )

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Unlink a device from the account; its readings are kept for the reports",
        description = "Removes the device from the account and answers 204 without body. The readings it already sent stay " +
            "in the history and in the reports."
    )
    fun deleteDevice(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        deviceCommandHandler.handleDelete(DeleteDeviceCommand(id, jwt.accountId()))
        return ResponseEntity.noContent().build()
    }
}
