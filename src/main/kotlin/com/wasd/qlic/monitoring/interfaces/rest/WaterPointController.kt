package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.CreateWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.DeleteWaterPointCommand
import com.wasd.qlic.monitoring.application.commands.UpdateWaterPointCommand
import com.wasd.qlic.monitoring.application.handlers.DeviceCommandHandler
import com.wasd.qlic.monitoring.application.handlers.LocationCommandHandler
import com.wasd.qlic.monitoring.application.handlers.WaterPointCommandHandler
import com.wasd.qlic.monitoring.application.queries.GetDevicesByAccountQuery
import com.wasd.qlic.monitoring.application.queries.GetLocationsQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterPointByIdQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterPointsQuery
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.domain.model.WaterPointType
import com.wasd.qlic.monitoring.interfaces.rest.resources.SaveWaterPointResource
import com.wasd.qlic.monitoring.interfaces.rest.resources.WaterPointResource
import com.wasd.qlic.monitoring.interfaces.rest.resources.toResource
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/water-points")
@Tag(name = "Water Points", description = "Device Monitoring - Catalog of the monitored Water Points of each location (US13)")
class WaterPointController(
    private val waterPointHandler: WaterPointCommandHandler,
    private val locationHandler: LocationCommandHandler,
    private val deviceHandler: DeviceCommandHandler
) {

    @PostMapping
    @Operation(
        summary = "Register a Water Point in a location (US13)",
        description = "The number of Water Points is limited by the plan in force (maxWaterPointsAllowed)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Water Point registered"),
        ApiResponse(
            responseCode = "404", description = "The location does not exist in the account",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "The plan does not allow more Water Points",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: SaveWaterPointResource
    ): ResponseEntity<WaterPointResource> {
        val accountId = jwt.accountId()
        val waterPoint = waterPointHandler.handle(
            CreateWaterPointCommand(
                accountId = accountId,
                locationId = requireNotNull(resource.locationId),
                name = requireNotNull(resource.name),
                type = resource.type ?: WaterPointType.OTHER
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(toResources(accountId, listOf(waterPoint)).single())
    }

    @GetMapping
    @Operation(
        summary = "List the Water Points of the account, optionally of one location (US13)",
        description = "Returns the Water Points of the account with their location and number of assigned devices. With " +
            "locationId only those of that location are returned."
    )
    fun list(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) locationId: UUID?
    ): ResponseEntity<List<WaterPointResource>> {
        val accountId = jwt.accountId()
        return ResponseEntity.ok(toResources(accountId, waterPointHandler.handle(GetWaterPointsQuery(accountId, locationId))))
    }

    @GetMapping("/{waterPointId}")
    @Operation(
        summary = "Get a Water Point of the account",
        description = "Returns the Water Point with its location and number of assigned devices. A Water Point of another " +
            "account returns 404."
    )
    fun get(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable waterPointId: UUID
    ): ResponseEntity<WaterPointResource> {
        val accountId = jwt.accountId()
        val waterPoint = waterPointHandler.handle(GetWaterPointByIdQuery(waterPointId, accountId))
        return ResponseEntity.ok(toResources(accountId, listOf(waterPoint)).single())
    }

    @PutMapping("/{waterPointId}")
    @Operation(
        summary = "Rename a Water Point, change its type or move it to another location",
        description = "Full replacement of the name, type and location of the Water Point. The devices assigned to it stay " +
            "assigned."
    )
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable waterPointId: UUID,
        @Valid @RequestBody resource: SaveWaterPointResource
    ): ResponseEntity<WaterPointResource> {
        val accountId = jwt.accountId()
        val waterPoint = waterPointHandler.handle(
            UpdateWaterPointCommand(
                waterPointId = waterPointId,
                accountId = accountId,
                locationId = requireNotNull(resource.locationId),
                name = requireNotNull(resource.name),
                type = resource.type ?: WaterPointType.OTHER
            )
        )
        return ResponseEntity.ok(toResources(accountId, listOf(waterPoint)).single())
    }

    @DeleteMapping("/{waterPointId}")
    @Operation(
        summary = "Delete a Water Point without assigned devices",
        description = "Deletes the Water Point and answers 204. When a device is still assigned to it, it returns 409; " +
            "reassign or unlink the device first."
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Water Point deleted"),
        ApiResponse(
            responseCode = "409", description = "A device is still assigned to the Water Point",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun delete(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable waterPointId: UUID
    ): ResponseEntity<Void> {
        waterPointHandler.handle(DeleteWaterPointCommand(waterPointId, jwt.accountId()))
        return ResponseEntity.noContent().build()
    }

    private fun toResources(accountId: UUID, waterPoints: List<WaterPoint>): List<WaterPointResource> {
        val locationNames = locationHandler.handle(GetLocationsQuery(accountId)).associate { it.id to it.name }
        val devicesByWaterPoint = deviceHandler.handleGetByAccount(GetDevicesByAccountQuery(accountId))
            .mapNotNull { it.waterPointId }
            .groupingBy { it }
            .eachCount()
        return waterPoints.map { it.toResource(locationNames[it.locationId], devicesByWaterPoint[it.id] ?: 0) }
    }
}
