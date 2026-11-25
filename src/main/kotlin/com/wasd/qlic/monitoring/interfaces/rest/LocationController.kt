package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.CreateLocationCommand
import com.wasd.qlic.monitoring.application.commands.DeleteLocationCommand
import com.wasd.qlic.monitoring.application.commands.UpdateLocationCommand
import com.wasd.qlic.monitoring.application.handlers.LocationCommandHandler
import com.wasd.qlic.monitoring.application.handlers.WaterPointCommandHandler
import com.wasd.qlic.monitoring.application.queries.GetLocationByIdQuery
import com.wasd.qlic.monitoring.application.queries.GetLocationsQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterPointsQuery
import com.wasd.qlic.monitoring.interfaces.rest.resources.LocationResource
import com.wasd.qlic.monitoring.interfaces.rest.resources.SaveLocationResource
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations", description = "Device Monitoring - Premises or homes that group the Water Points (US13)")
class LocationController(
    private val locationHandler: LocationCommandHandler,
    private val waterPointHandler: WaterPointCommandHandler
) {

    @PostMapping
    @Operation(summary = "Register a location of the account (US13)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Location registered"),
        ApiResponse(
            responseCode = "400", description = "Missing or invalid name",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: SaveLocationResource
    ): ResponseEntity<LocationResource> {
        val location = locationHandler.handle(
            CreateLocationCommand(jwt.accountId(), requireNotNull(resource.name), resource.address)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(location.toResource(0))
    }

    @GetMapping
    @Operation(summary = "List the locations of the account with their number of Water Points (US13)")
    fun list(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<LocationResource>> {
        val accountId = jwt.accountId()
        val countByLocation = waterPointHandler.handle(GetWaterPointsQuery(accountId)).groupingBy { it.locationId }.eachCount()
        return ResponseEntity.ok(
            locationHandler.handle(GetLocationsQuery(accountId)).map { it.toResource(countByLocation[it.id] ?: 0) }
        )
    }

    @GetMapping("/{locationId}")
    @Operation(summary = "Get a location of the account")
    fun get(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable locationId: UUID
    ): ResponseEntity<LocationResource> {
        val accountId = jwt.accountId()
        val location = locationHandler.handle(GetLocationByIdQuery(locationId, accountId))
        val count = waterPointHandler.handle(GetWaterPointsQuery(accountId, locationId)).size
        return ResponseEntity.ok(location.toResource(count))
    }

    @PutMapping("/{locationId}")
    @Operation(summary = "Rename a location or change its address")
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable locationId: UUID,
        @Valid @RequestBody resource: SaveLocationResource
    ): ResponseEntity<LocationResource> {
        val accountId = jwt.accountId()
        val location = locationHandler.handle(
            UpdateLocationCommand(locationId, accountId, requireNotNull(resource.name), resource.address)
        )
        val count = waterPointHandler.handle(GetWaterPointsQuery(accountId, locationId)).size
        return ResponseEntity.ok(location.toResource(count))
    }

    @DeleteMapping("/{locationId}")
    @Operation(summary = "Delete a location without Water Points")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Location deleted"),
        ApiResponse(
            responseCode = "409", description = "The location still groups Water Points",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun delete(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable locationId: UUID
    ): ResponseEntity<Void> {
        locationHandler.handle(DeleteLocationCommand(locationId, jwt.accountId()))
        return ResponseEntity.noContent().build()
    }
}
