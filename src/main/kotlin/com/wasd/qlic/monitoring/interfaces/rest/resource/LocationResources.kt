package com.wasd.qlic.monitoring.interfaces.rest.resources

import com.wasd.qlic.monitoring.domain.model.Location
import com.wasd.qlic.monitoring.domain.model.WaterPoint
import com.wasd.qlic.monitoring.domain.model.WaterPointType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Premises or home of the subscriber (US13).")
data class SaveLocationResource(
    @field:NotBlank(message = "{validation.location.name.required}")
    @field:Size(max = 80, message = "{validation.name.size}")
    @field:Schema(example = "Restaurante Don Lucho - Miraflores")
    val name: String?,
    @field:Size(max = 160, message = "{validation.address.size}")
    @field:Schema(example = "Av. Larco 345, Miraflores", nullable = true)
    val address: String? = null
)

@Schema(description = "Location of the account with the number of Water Points it groups.")
data class LocationResource(
    val id: UUID,
    val name: String,
    val address: String?,
    val waterPointsCount: Int,
    val createdAt: Instant
)

@Schema(description = "Water Point of a location (US13). The number of Water Points is limited by the plan.")
data class SaveWaterPointResource(
    @field:NotNull(message = "{validation.water-point.location.required}")
    val locationId: UUID?,
    @field:NotBlank(message = "{validation.water-point.name.required}")
    @field:Size(max = 80, message = "{validation.name.size}")
    @field:Schema(example = "Cocina principal")
    val name: String?,
    @field:Schema(example = "KITCHEN", defaultValue = "OTHER")
    val type: WaterPointType? = WaterPointType.OTHER
)

@Schema(description = "Water Point with its location and the devices assigned to it.")
data class WaterPointResource(
    val id: UUID,
    val locationId: UUID,
    val locationName: String?,
    val name: String,
    val type: WaterPointType,
    val devicesCount: Int,
    val createdAt: Instant
)

fun Location.toResource(waterPointsCount: Int) = LocationResource(id, name, address, waterPointsCount, createdAt)

fun WaterPoint.toResource(locationName: String?, devicesCount: Int) =
    WaterPointResource(id, locationId, locationName, name, type, devicesCount, createdAt)
