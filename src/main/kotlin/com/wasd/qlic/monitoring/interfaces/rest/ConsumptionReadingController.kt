package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.RegisterConsumptionReadingCommand
import com.wasd.qlic.monitoring.application.handlers.ReadingQueryHandler
import com.wasd.qlic.monitoring.application.handlers.RegisterConsumptionReadingCommandHandler
import com.wasd.qlic.monitoring.application.queries.GetReadingsByPeriodQuery
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Consumption reading sent by a device. recordedAt defaults to the reception time.")
data class RegisterConsumptionReadingResource(
    @field:NotNull(message = "{validation.reading.device.required}")
    val deviceId: UUID?,

    @field:NotNull(message = "{validation.reading.volume.required}")
    @field:DecimalMin(value = "0.0", message = "{validation.reading.volume.min}")
    @field:Schema(example = "12.5")
    val volumeLiters: BigDecimal?,

    @field:Schema(example = "2026-11-08T23:15:00", nullable = true)
    val recordedAt: LocalDateTime? = null
)

@Schema(description = "Registered consumption reading.")
data class ConsumptionReadingResource(
    val id: UUID,
    val deviceId: UUID,
    val waterPointId: UUID,
    val volumeLiters: BigDecimal,
    val recordedAt: LocalDateTime
)

@RestController
@RequestMapping("/api/v1/consumption-readings")
@Tag(name = "Consumption Readings", description = "Device Monitoring - Ingestion and history of the consumption readings of the devices")
class ConsumptionReadingController(
    private val handler: RegisterConsumptionReadingCommandHandler,
    private val readingQueryHandler: ReadingQueryHandler
) {

    @PostMapping
    @Operation(
        summary = "Register a consumption reading of a device",
        description = "The reading is stored under the Water Point currently assigned to the device. It feeds the consumption reports (US24)."
    )
    fun register(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: RegisterConsumptionReadingResource
    ): ResponseEntity<ConsumptionReadingResource> {
        val reading = handler.handle(
            RegisterConsumptionReadingCommand(
                accountId = jwt.accountId(),
                deviceId = requireNotNull(resource.deviceId),
                volumeLiters = requireNotNull(resource.volumeLiters),
                recordedAt = resource.recordedAt
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(reading.toResource())
    }

    @GetMapping
    @Operation(
        summary = "Readings history of a period (US13 - Scenario 1)",
        description = "Most recent first. Filter by locationId to get only the readings of one location, or by waterPointId. " +
            "The period cannot exceed 92 days."
    )
    fun history(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-01") from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-30") to: LocalDate,
        @RequestParam(required = false) locationId: UUID?,
        @RequestParam(required = false) waterPointId: UUID?
    ): ResponseEntity<List<ConsumptionReadingResource>> =
        ResponseEntity.ok(
            readingQueryHandler.handle(GetReadingsByPeriodQuery(jwt.accountId(), from, to, locationId, waterPointId))
                .map { it.toResource() }
        )

    private fun ConsumptionReading.toResource() =
        ConsumptionReadingResource(id, deviceId, waterPointId, volumeLiters, recordedAt)
}
