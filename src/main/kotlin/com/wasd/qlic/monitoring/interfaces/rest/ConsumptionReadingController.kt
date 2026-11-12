package com.wasd.qlic.monitoring.interfaces.rest

import com.wasd.qlic.monitoring.application.commands.RegisterConsumptionReadingCommand
import com.wasd.qlic.monitoring.application.handlers.RegisterConsumptionReadingCommandHandler
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
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
@Tag(name = "Consumption Readings", description = "Device Monitoring - Ingestion of the consumption readings of the devices")
class ConsumptionReadingController(
    private val handler: RegisterConsumptionReadingCommandHandler
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
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ConsumptionReadingResource(reading.id, reading.deviceId, reading.waterPointId, reading.volumeLiters, reading.recordedAt)
        )
    }
}
