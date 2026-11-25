package com.wasd.qlic.consumption.interfaces.rest

import com.wasd.qlic.consumption.application.commands.DefineSustainabilityGoalCommand
import com.wasd.qlic.consumption.application.commands.DefineTariffCommand
import com.wasd.qlic.consumption.application.handlers.ConsumptionReportQueryHandler
import com.wasd.qlic.consumption.application.handlers.SustainabilityGoalHandler
import com.wasd.qlic.consumption.application.handlers.TariffHandler
import com.wasd.qlic.consumption.application.queries.GetPeriodComparisonQuery
import com.wasd.qlic.consumption.application.queries.GetPeriodReportQuery
import com.wasd.qlic.consumption.application.queries.GetSustainabilityGoalQuery
import com.wasd.qlic.consumption.application.queries.GetTariffQuery
import com.wasd.qlic.consumption.domain.exceptions.InvalidPeriodException
import com.wasd.qlic.consumption.interfaces.rest.resources.DefineSustainabilityGoalResource
import com.wasd.qlic.consumption.interfaces.rest.resources.DefineTariffResource
import com.wasd.qlic.consumption.interfaces.rest.resources.PeriodComparisonResource
import com.wasd.qlic.consumption.interfaces.rest.resources.PeriodConsumptionReportResource
import com.wasd.qlic.consumption.interfaces.rest.resources.ShareableReportResource
import com.wasd.qlic.consumption.interfaces.rest.resources.SustainabilityGoalResource
import com.wasd.qlic.consumption.interfaces.rest.resources.TariffResource
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/v1/consumption")
@Tag(
    name = "Consumption Analytics",
    description = "Consumption Analytics - Period report, comparison, estimated cost, recommendations, sharing and goals"
)
class ConsumptionAnalyticsController(
    private val reportHandler: ConsumptionReportQueryHandler,
    private val tariffHandler: TariffHandler,
    private val goalHandler: SustainabilityGoalHandler,
    private val assembler: ConsumptionReportAssembler
) {

    @GetMapping("/reports/period")
    @Operation(
        summary = "Consumption report of the period (US21, US23, US25, US27)",
        description = "Accumulated consumption and its distribution by Water Point, referential estimated cost, saving " +
            "recommendations and the progress of the goal of the month. Without dates, the current month up to today."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Report of the period; hasData = false when there are no readings"),
        ApiResponse(
            responseCode = "400", description = "Invalid period",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun periodReport(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-01") from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(example = "2026-11-30") to: LocalDate?
    ): ResponseEntity<PeriodConsumptionReportResource> =
        ResponseEntity.ok(assembler.toResource(reportHandler.handle(GetPeriodReportQuery(jwt.accountId(), from, to))))

    @GetMapping("/reports/comparison")
    @Operation(
        summary = "Compare the consumption with the previous period (US22)",
        description = "Without dates, the current month up to today against the same days of the previous month. With dates, " +
            "against the previous period of the same length. comparable = false when there is not enough history."
    )
    fun comparison(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ResponseEntity<PeriodComparisonResource> =
        ResponseEntity.ok(assembler.toResource(reportHandler.handle(GetPeriodComparisonQuery(jwt.accountId(), from, to))))

    @GetMapping("/reports/period/share")
    @Operation(
        summary = "Summary of the period report to share (US26)",
        description = "Localized plain text that the app sends to the share options of the device (WhatsApp, email, etc.)."
    )
    fun share(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?
    ): ResponseEntity<ShareableReportResource> =
        ResponseEntity.ok(assembler.toShareable(reportHandler.handle(GetPeriodReportQuery(jwt.accountId(), from, to))))

    @GetMapping("/tariff")
    @Operation(
        summary = "Tariff applied to the estimated cost (US23)",
        description = "By default the category follows the plan (PYME plans are commercial) with the reference price of that category."
    )
    fun tariff(@Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt): ResponseEntity<TariffResource> =
        ResponseEntity.ok(assembler.toResource(tariffHandler.handle(GetTariffQuery(jwt.accountId()))))

    @PutMapping("/tariff")
    @Operation(summary = "Set the service category and, optionally, the own price per cubic meter (US23)")
    fun defineTariff(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody resource: DefineTariffResource
    ): ResponseEntity<TariffResource> =
        ResponseEntity.ok(
            assembler.toResource(
                tariffHandler.handle(
                    DefineTariffCommand(jwt.accountId(), requireNotNull(resource.category), resource.customPricePerCubicMeter)
                )
            )
        )

    @PutMapping("/goals/{month}")
    @Operation(
        summary = "Define the reduction goal of a month (US27 - Scenario 1)",
        description = "The reduction is measured against the consumption of the previous month, which must have readings."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Goal stored with its current progress"),
        ApiResponse(
            responseCode = "409", description = "The previous month has no readings to measure the reduction",
            content = [Content(schema = Schema(implementation = ProblemDetail::class))]
        )
    )
    fun defineGoal(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable @Parameter(example = "2026-11") month: String,
        @Valid @RequestBody resource: DefineSustainabilityGoalResource
    ): ResponseEntity<SustainabilityGoalResource> =
        ResponseEntity.ok(
            assembler.toResource(
                goalHandler.handle(
                    DefineSustainabilityGoalCommand(jwt.accountId(), parseMonth(month), requireNotNull(resource.reductionPercentage))
                )
            )
        )

    @GetMapping("/goals/{month}")
    @Operation(
        summary = "Progress of the goal of a month (US27)",
        description = "status = ACHIEVED when the month is over and the consumption stayed at or below the target (Scenario 2)."
    )
    fun goal(
        @Parameter(hidden = true) @AuthenticationPrincipal jwt: Jwt,
        @PathVariable @Parameter(example = "2026-11") month: String
    ): ResponseEntity<SustainabilityGoalResource> =
        ResponseEntity.ok(assembler.toResource(goalHandler.handle(GetSustainabilityGoalQuery(jwt.accountId(), parseMonth(month)))))

    private fun parseMonth(month: String): YearMonth = try {
        YearMonth.parse(month)
    } catch (exception: DateTimeParseException) {
        throw InvalidPeriodException()
    }
}
