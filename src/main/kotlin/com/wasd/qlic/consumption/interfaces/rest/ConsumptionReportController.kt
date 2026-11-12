package com.wasd.qlic.consumption.interfaces.rest

import com.wasd.qlic.consumption.application.commands.DefineActivityScheduleCommand
import com.wasd.qlic.consumption.application.handlers.ActivityConsumptionQueryHandler
import com.wasd.qlic.consumption.application.handlers.DefineActivityScheduleCommandHandler
import com.wasd.qlic.consumption.application.queries.GetActivityConsumptionReportQuery
import com.wasd.qlic.consumption.application.queries.GetActivityScheduleQuery
import com.wasd.qlic.consumption.domain.model.ActivitySchedule
import com.wasd.qlic.consumption.domain.model.PremisesActivitySchedule
import com.wasd.qlic.consumption.interfaces.rest.resources.ActivityConsumptionReportResource
import com.wasd.qlic.consumption.interfaces.rest.resources.ActivityScheduleResource
import com.wasd.qlic.consumption.interfaces.rest.resources.WaterPointActivityConsumptionResource
import com.wasd.qlic.shared.interfaces.rest.security.accountId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/consumption")
@Tag(name = "Consumption Reports", description = "Consumption Analytics - Consumption outside the activity hours (US24)")
class ConsumptionReportController(
    private val defineActivityScheduleCommandHandler: DefineActivityScheduleCommandHandler,
    private val activityConsumptionQueryHandler: ActivityConsumptionQueryHandler
) {

    @PutMapping("/activity-schedule")
    @Operation(
        summary = "Declare the activity schedule of the premises (US24)",
        description = "Opening and closing time and the active days. A closing time before the opening time crosses midnight."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Schedule stored"),
        ApiResponse(
            responseCode = "400", description = "Missing or invalid times or days",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun defineSchedule(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: ActivityScheduleResource
    ): ResponseEntity<ActivityScheduleResource> {
        val stored = defineActivityScheduleCommandHandler.handle(
            DefineActivityScheduleCommand(
                accountId = jwt.accountId(),
                openingTime = requireNotNull(resource.openingTime),
                closingTime = requireNotNull(resource.closingTime),
                activeDays = requireNotNull(resource.activeDays)
            )
        )
        return ResponseEntity.ok(stored.toResource())
    }

    @GetMapping("/activity-schedule")
    @Operation(summary = "Get the activity schedule of the premises (US24)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Declared schedule"),
        ApiResponse(
            responseCode = "404", description = "The schedule has not been declared",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun getSchedule(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<ActivityScheduleResource> =
        ResponseEntity.ok(activityConsumptionQueryHandler.handle(GetActivityScheduleQuery(jwt.accountId())).toResource())

    @GetMapping("/reports/activity-hours")
    @Operation(
        summary = "Consumption inside and outside the activity hours (US24 - Scenario 1)",
        description = "Aggregates the readings of the period by Water Point and differentiates the volume consumed while " +
            "the premises is closed. Without a declared schedule, only totals are returned (scheduleDefined = false)."
    )
    fun getActivityReport(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-01") from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-10") to: LocalDate
    ): ResponseEntity<ActivityConsumptionReportResource> {
        val report = activityConsumptionQueryHandler.handle(GetActivityConsumptionReportQuery(jwt.accountId(), from, to))
        return ResponseEntity.ok(
            ActivityConsumptionReportResource(
                from = report.period.from,
                to = report.period.to,
                scheduleDefined = report.scheduleDefined,
                schedule = report.schedule?.toResource(null),
                totalLiters = report.totalLiters,
                withinActivityLiters = report.withinActivityLiters,
                outsideActivityLiters = report.outsideActivityLiters,
                outsideActivityPercentage = report.outsideActivityPercentage,
                byWaterPoint = report.byWaterPoint.map {
                    WaterPointActivityConsumptionResource(it.waterPointId, it.totalLiters, it.withinActivityLiters, it.outsideActivityLiters)
                }
            )
        )
    }

    private fun PremisesActivitySchedule.toResource() = schedule.toResource(updatedAt)

    private fun ActivitySchedule.toResource(updatedAt: java.time.Instant?) =
        ActivityScheduleResource(openingTime, closingTime, activeDays.sorted().toSet(), updatedAt)
}
