package com.wasd.qlic.support.interfaces.rest

import com.wasd.qlic.support.application.commands.AssignTechnicianCommand
import com.wasd.qlic.support.application.commands.ResolveIncidentCommand
import com.wasd.qlic.support.application.handlers.AssignTechnicianCommandHandler
import com.wasd.qlic.support.application.handlers.ResolveIncidentCommandHandler
import com.wasd.qlic.support.application.handlers.SupportQueryHandler
import com.wasd.qlic.support.interfaces.rest.resources.AssignTechnicianResource
import com.wasd.qlic.support.interfaces.rest.resources.IncidentResource
import com.wasd.qlic.support.interfaces.rest.resources.ResolveIncidentResource
import com.wasd.qlic.support.interfaces.rest.resources.TechnicianResource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Endpoints used by the support staff to register the attention of the incidents. */
@RestController
@Tag(name = "Support Staff", description = "Support - Technician assignment and resolution of incidents (US36)")
class TechnicianController(
    private val assignTechnicianCommandHandler: AssignTechnicianCommandHandler,
    private val resolveIncidentCommandHandler: ResolveIncidentCommandHandler,
    private val supportQueryHandler: SupportQueryHandler
) {

    @GetMapping("/api/v1/technicians")
    @Operation(summary = "List the active support technicians")
    fun getTechnicians(): ResponseEntity<List<TechnicianResource>> =
        ResponseEntity.ok(
            supportQueryHandler.activeTechnicians().map { TechnicianResource(it.id, it.fullName, it.phone, it.zone) }
        )

    @PostMapping("/api/v1/incidents/{incidentId}/technical-visit/assignment")
    @Operation(
        summary = "Assign a technician to a requested visit (US36 - Scenario 2)",
        description = "Registers the technician and the scheduled date, notifies the subscriber and moves the incident to IN_PROGRESS."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Technician assigned"),
        ApiResponse(
            responseCode = "409", description = "The incident has no requested visit",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun assignTechnician(
        @PathVariable incidentId: UUID,
        @Valid @RequestBody resource: AssignTechnicianResource
    ): ResponseEntity<IncidentResource> {
        val incident = assignTechnicianCommandHandler.handle(
            AssignTechnicianCommand(incidentId, resource.technicianId, requireNotNull(resource.scheduledAt))
        )
        return ResponseEntity.ok(incident.toResource())
    }

    @PostMapping("/api/v1/incidents/{incidentId}/resolution")
    @Operation(summary = "Register the resolution of an incident", description = "Moves the incident to RESOLVED.")
    fun resolve(
        @PathVariable incidentId: UUID,
        @Valid @RequestBody resource: ResolveIncidentResource
    ): ResponseEntity<IncidentResource> =
        ResponseEntity.ok(resolveIncidentCommandHandler.handle(ResolveIncidentCommand(incidentId, resource.description)).toResource())
}
