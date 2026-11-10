package com.wasd.qlic.support.interfaces.rest

import com.wasd.qlic.shared.interfaces.rest.security.accountId
import com.wasd.qlic.shared.interfaces.rest.security.subscriberId
import com.wasd.qlic.support.application.commands.RegisterIncidentCommand
import com.wasd.qlic.support.application.commands.RequestTechnicalVisitCommand
import com.wasd.qlic.support.application.handlers.RegisterIncidentCommandHandler
import com.wasd.qlic.support.application.handlers.RequestTechnicalVisitCommandHandler
import com.wasd.qlic.support.application.handlers.SupportQueryHandler
import com.wasd.qlic.support.application.queries.GetIncidentByIdQuery
import com.wasd.qlic.support.application.queries.GetIncidentsByAccountQuery
import com.wasd.qlic.support.interfaces.rest.resources.IncidentResource
import com.wasd.qlic.support.interfaces.rest.resources.RegisterIncidentResource
import com.wasd.qlic.support.interfaces.rest.resources.RequestTechnicalVisitResource
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Support - Register incidents, request technical visits and follow them up (US35, US36, US37)")
class IncidentController(
    private val registerIncidentCommandHandler: RegisterIncidentCommandHandler,
    private val requestTechnicalVisitCommandHandler: RequestTechnicalVisitCommandHandler,
    private val supportQueryHandler: SupportQueryHandler
) {

    @PostMapping
    @Operation(
        summary = "Register an incident (US35)",
        description = "Creates the incident in PENDING state. Send alertId to register it from an active alert (Scenario 2)."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Incident registered"),
        ApiResponse(
            responseCode = "400", description = "Empty description or no device, Water Point or alert selected",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "404", description = "The device or alert does not belong to the account",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "The alert is no longer active",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun register(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: RegisterIncidentResource
    ): ResponseEntity<IncidentResource> {
        val incident = registerIncidentCommandHandler.handle(
            RegisterIncidentCommand(
                accountId = jwt.accountId(),
                reportedBy = jwt.subscriberId(),
                description = resource.description,
                deviceId = resource.deviceId,
                waterPointId = resource.waterPointId,
                alertId = resource.alertId
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(incident.toResource())
    }

    @GetMapping
    @Operation(
        summary = "List the incidents of the account (US37)",
        description = "Each incident includes its registration date, associated device and current state, newest first."
    )
    fun getIncidents(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<List<IncidentResource>> =
        ResponseEntity.ok(supportQueryHandler.handle(GetIncidentsByAccountQuery(jwt.accountId())).map { it.toResource() })

    @GetMapping("/{incidentId}")
    @Operation(summary = "Get the detail of an incident (US37)")
    fun getIncident(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable incidentId: UUID
    ): ResponseEntity<IncidentResource> =
        ResponseEntity.ok(supportQueryHandler.handle(GetIncidentByIdQuery(incidentId, jwt.accountId())).toResource())

    @PostMapping("/{incidentId}/technical-visit")
    @Operation(
        summary = "Request a technical visit (US36 - Scenario 1)",
        description = "Requires a PENDING incident and a plan or trial that covers technical visits. The incident moves to VISIT_REQUESTED."
    )
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Visit requested"),
        ApiResponse(
            responseCode = "403", description = "The current plan does not cover technical visits",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "The incident is not PENDING",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun requestTechnicalVisit(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable incidentId: UUID,
        @Valid @RequestBody resource: RequestTechnicalVisitResource
    ): ResponseEntity<IncidentResource> {
        val incident = requestTechnicalVisitCommandHandler.handle(
            RequestTechnicalVisitCommand(
                incidentId = incidentId,
                accountId = jwt.accountId(),
                preferredDate = requireNotNull(resource.preferredDate),
                fromTime = requireNotNull(resource.fromTime),
                toTime = requireNotNull(resource.toTime),
                notes = resource.notes
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(incident.toResource())
    }
}
