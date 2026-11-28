package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.DeleteWaterTankCommand
import com.wasd.qlic.monitoring.application.commands.RegisterTankLevelCommand
import com.wasd.qlic.monitoring.application.commands.SaveWaterTankCommand
import com.wasd.qlic.monitoring.application.handlers.LocationCommandHandler
import com.wasd.qlic.monitoring.application.handlers.WaterTankCommandHandler
import com.wasd.qlic.monitoring.application.queries.GetLocationsQuery
import com.wasd.qlic.monitoring.application.queries.GetTankLevelsQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterTankQuery
import com.wasd.qlic.monitoring.application.queries.GetWaterTanksQuery
import com.wasd.qlic.monitoring.domain.model.ReplenishmentPrediction
import com.wasd.qlic.monitoring.domain.model.TankLevelReading
import com.wasd.qlic.monitoring.domain.model.TankStatus
import com.wasd.qlic.monitoring.domain.model.WaterTank
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Tank of a location monitored by a device (US28).")
data class SaveWaterTankResource(
    @field:NotNull(message = "{validation.tank.location.required}")
    val locationId: UUID?,
    @field:NotBlank(message = "{validation.tank.name.required}")
    @field:Size(max = 80, message = "{validation.name.size}")
    @field:Schema(example = "Tanque elevado")
    val name: String?,
    @field:NotNull(message = "{validation.tank.capacity.required}")
    @field:DecimalMin(value = "1", message = "{validation.tank.capacity.min}")
    @field:Schema(example = "1100")
    val capacityLiters: BigDecimal?,
    @field:DecimalMin(value = "1", message = "{validation.tank.critical.range}")
    @field:DecimalMax(value = "99", message = "{validation.tank.critical.range}")
    @field:Schema(example = "20", defaultValue = "20")
    val criticalLevelPercentage: BigDecimal? = null,
    @field:Schema(nullable = true, description = "Device of the account that reports the level")
    val deviceId: UUID? = null
)

@Schema(description = "Level reported by the device of the tank. recordedAt defaults to the reception time.")
data class RegisterTankLevelResource(
    @field:NotNull(message = "{validation.tank.level.required}")
    @field:DecimalMin(value = "0", message = "{validation.tank.level.range}")
    @field:DecimalMax(value = "100", message = "{validation.tank.level.range}")
    @field:Schema(example = "64.5")
    val levelPercentage: BigDecimal?,
    @field:Schema(example = "2026-11-20T08:00:00", nullable = true)
    val recordedAt: LocalDateTime? = null
)

@Schema(description = "Moment in which the tank is expected to reach its critical level (US30).")
data class ReplenishmentPredictionResource(
    @field:Schema(description = "False while there is not enough history (US30 - Scenario 2)") val available: Boolean,
    @field:Schema(nullable = true) val predictedCriticalAt: LocalDateTime?,
    @field:Schema(example = "36.5", nullable = true) val hoursToCritical: BigDecimal?,
    @field:Schema(example = "1.20", nullable = true) val consumptionPercentagePerHour: BigDecimal?
)

@Schema(description = "Tank with its current level (US28) and, in the detail, its replenishment prediction (US30).")
data class WaterTankResource(
    val id: UUID,
    val name: String,
    val locationId: UUID,
    @field:Schema(nullable = true) val locationName: String?,
    @field:Schema(example = "1100.00") val capacityLiters: BigDecimal,
    @field:Schema(example = "20.00") val criticalLevelPercentage: BigDecimal,
    @field:Schema(nullable = true) val deviceId: UUID?,
    val status: TankStatus,
    @field:Schema(example = "64.50", nullable = true) val currentLevelPercentage: BigDecimal?,
    @field:Schema(example = "709.50", nullable = true) val availableLiters: BigDecimal?,
    @field:Schema(nullable = true) val lastReadingAt: LocalDateTime?,
    @field:Schema(nullable = true, description = "Only in the detail of the tank") val prediction: ReplenishmentPredictionResource?
)

@Schema(description = "Registered level; lowLevelAlertRaised is true when this reading crossed the critical level (US29).")
data class TankLevelRegistrationResource(
    val tank: WaterTankResource,
    @field:Schema(example = "18.00") val levelPercentage: BigDecimal,
    val recordedAt: LocalDateTime,
    val lowLevelAlertRaised: Boolean
)

@Schema(description = "Level of the tank at a moment.")
data class TankLevelResource(
    @field:Schema(example = "64.50") val levelPercentage: BigDecimal,
    val recordedAt: LocalDateTime
)

@RestController
@RequestMapping("/api/v1/tanks")
@Tag(name = "Water Tanks", description = "Device Monitoring - Level of the tanks, low level alert and replenishment prediction")
class WaterTankController(
    private val tankHandler: WaterTankCommandHandler,
    private val locationHandler: LocationCommandHandler
) {

    @PostMapping
    @Operation(
        summary = "Register a tank in a location (US28)",
        description = "Creates a tank in a location of the account with its capacity in liters and its critical level (20% " +
            "by default). Optionally links the device that reports its level."
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: SaveWaterTankResource
    ): ResponseEntity<WaterTankResource> {
        val accountId = jwt.accountId()
        val tank = tankHandler.handle(resource.toCommand(accountId, null))
        return ResponseEntity.status(HttpStatus.CREATED).body(tank.toResource(locationNames(accountId), null))
    }

    @GetMapping
    @Operation(
        summary = "List the tanks of the account with their current level (US28)",
        description = "Returns every tank with its status, current level and available liters, calculated from the last " +
            "reported level."
    )
    fun list(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) locationId: UUID?
    ): ResponseEntity<List<WaterTankResource>> {
        val accountId = jwt.accountId()
        val names = locationNames(accountId)
        return ResponseEntity.ok(tankHandler.handle(GetWaterTanksQuery(accountId, locationId)).map { it.toResource(names, null) })
    }

    @GetMapping("/{tankId}")
    @Operation(
        summary = "Detail of a tank with its replenishment prediction (US28, US30)",
        description = "Current level, date and time of the last reading and the estimated moment of reaching the critical level."
    )
    fun get(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable tankId: UUID
    ): ResponseEntity<WaterTankResource> {
        val accountId = jwt.accountId()
        val detail = tankHandler.handle(GetWaterTankQuery(tankId, accountId))
        return ResponseEntity.ok(detail.tank.toResource(locationNames(accountId), detail.prediction))
    }

    @PutMapping("/{tankId}")
    @Operation(
        summary = "Update the data, critical level or device of a tank",
        description = "Full replacement of the data of the tank: location, name, capacity, critical level and device. The " +
            "level history is kept."
    )
    fun update(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable tankId: UUID,
        @Valid @RequestBody resource: SaveWaterTankResource
    ): ResponseEntity<WaterTankResource> {
        val accountId = jwt.accountId()
        val tank = tankHandler.handle(resource.toCommand(accountId, tankId))
        return ResponseEntity.ok(tank.toResource(locationNames(accountId), null))
    }

    @DeleteMapping("/{tankId}")
    @Operation(
        summary = "Delete a tank and its level history",
        description = "Removes the tank and every level registered for it. Answers 204 without body."
    )
    fun delete(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable tankId: UUID
    ): ResponseEntity<Void> {
        tankHandler.handle(DeleteWaterTankCommand(tankId, jwt.accountId()))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{tankId}/levels")
    @Operation(
        summary = "Register the level reported by the device of the tank (US28, US29)",
        description = "When the level falls below the critical value, TankLevelLowEvent is published once and a push " +
            "notification is sent to the subscriber; the alert is rearmed when the tank is refilled."
    )
    @ResponseStatus(HttpStatus.CREATED)
    fun registerLevel(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable tankId: UUID,
        @Valid @RequestBody resource: RegisterTankLevelResource
    ): ResponseEntity<TankLevelRegistrationResource> {
        val accountId = jwt.accountId()
        val result = tankHandler.handle(
            RegisterTankLevelCommand(tankId, accountId, requireNotNull(resource.levelPercentage), resource.recordedAt)
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(
            TankLevelRegistrationResource(
                tank = result.tank.toResource(locationNames(accountId), null),
                levelPercentage = result.reading.levelPercentage,
                recordedAt = result.reading.recordedAt,
                lowLevelAlertRaised = result.lowLevelAlertRaised
            )
        )
    }

    @GetMapping("/{tankId}/levels")
    @Operation(
        summary = "Level history of a tank (at most 92 days)",
        description = "Levels reported between from and to (inclusive, format yyyy-MM-dd), oldest first. A period longer " +
            "than 92 days or with from after to returns 400."
    )
    fun levels(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable tankId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-01") from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-30") to: LocalDate
    ): ResponseEntity<List<TankLevelResource>> =
        ResponseEntity.ok(
            tankHandler.handle(GetTankLevelsQuery(tankId, jwt.accountId(), from, to)).map { it.toResource() }
        )

    private fun locationNames(accountId: UUID): Map<UUID, String> =
        locationHandler.handle(GetLocationsQuery(accountId)).associate { it.id to it.name }

    private fun SaveWaterTankResource.toCommand(accountId: UUID, tankId: UUID?) = SaveWaterTankCommand(
        accountId = accountId,
        locationId = requireNotNull(locationId),
        name = requireNotNull(name),
        capacityLiters = requireNotNull(capacityLiters),
        criticalLevelPercentage = criticalLevelPercentage ?: DEFAULT_CRITICAL_LEVEL,
        deviceId = deviceId,
        tankId = tankId
    )

    private fun WaterTank.toResource(locationNames: Map<UUID, String>, prediction: ReplenishmentPrediction?) = WaterTankResource(
        id = id,
        name = name,
        locationId = locationId,
        locationName = locationNames[locationId],
        capacityLiters = capacityLiters,
        criticalLevelPercentage = criticalLevelPercentage,
        deviceId = deviceId,
        status = status,
        currentLevelPercentage = currentLevelPercentage,
        availableLiters = availableLiters,
        lastReadingAt = lastReadingAt,
        prediction = prediction?.let {
            ReplenishmentPredictionResource(it.available, it.predictedCriticalAt, it.hoursToCritical, it.consumptionPercentagePerHour)
        }
    )

    private fun TankLevelReading.toResource() = TankLevelResource(levelPercentage, recordedAt)

    private companion object {
        val DEFAULT_CRITICAL_LEVEL = BigDecimal(20)
    }
}
