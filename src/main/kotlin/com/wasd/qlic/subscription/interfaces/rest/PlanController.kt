package com.wasd.qlic.subscriptions.interfaces.rest

import com.wasd.qlic.subscriptions.domain.model.PlanSegment
import com.wasd.qlic.subscriptions.domain.model.PlanType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@Schema(description = "A plan of the catalog with its monthly price and the limits it enables.")
data class PlanResource(
    @field:Schema(description = "Code used to purchase or change the plan", example = "HOME_PRO")
    val plan: PlanType,
    @field:Schema(description = "TRIAL, HOME or BUSINESS", example = "HOME")
    val segment: PlanSegment,
    @field:Schema(description = "Monthly price in soles", example = "49.90")
    val monthlyPrice: BigDecimal,
    @field:Schema(description = "ISO 4217 currency of the price", example = "PEN")
    val currency: String,
    @field:Schema(description = "Days covered by each payment (14 for the trial)", example = "30")
    val billingPeriodDays: Long,
    @field:Schema(description = "Maximum Water Points that can be monitored", example = "6")
    val maxWaterPoints: Int,
    @field:Schema(description = "Additional users that can be invited (US08)", example = "3")
    val maxAuthorizedUsers: Int,
    @field:Schema(description = "Whether technical visits are covered (US36)", example = "true")
    val technicalVisitsIncluded: Boolean,
    @field:Schema(description = "False only for TRIAL, which is started with POST /api/v1/subscriptions/trial", example = "true")
    val purchasable: Boolean
)

/** Public catalog of plans (TS07 - Scenario 1), used by the plans screen of the apps and by the Landing Page. */
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans", description = "Subscriptions - Public catalog of plans with prices and limits (TS07, US31)")
@SecurityRequirements
class PlanController {

    @GetMapping
    @Operation(
        summary = "List the plans of the catalog (TS07 - Scenario 1)",
        description = "Public endpoint, no token required. Returns every plan with its monthly price in soles, billing period " +
            "and limits, ordered by price. Filter by segment with ?segment=HOME or ?segment=BUSINESS; set includeTrial=false " +
            "to hide the free trial."
    )
    @ApiResponses(ApiResponse(responseCode = "200", description = "Plans of the catalog"))
    fun getPlans(
        @Parameter(description = "Only the plans of this segment", example = "HOME")
        @RequestParam(required = false) segment: PlanSegment?,
        @Parameter(description = "Include the free trial in the result", example = "true")
        @RequestParam(defaultValue = "true") includeTrial: Boolean
    ): ResponseEntity<List<PlanResource>> =
        ResponseEntity.ok(
            PlanType.entries
                .filter { segment == null || it.segment == segment }
                .filter { includeTrial || it.purchasable }
                .sortedBy { it.monthlyPriceSoles }
                .map { it.toResource() }
        )

    @GetMapping("/{plan}")
    @Operation(
        summary = "Get one plan of the catalog (TS07)",
        description = "Public endpoint, no token required. An unknown plan code returns 400."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "The plan"),
        ApiResponse(responseCode = "400", description = "The plan code does not exist")
    )
    fun getPlan(
        @Parameter(description = "Plan code", example = "PYME_BUSINESS") @PathVariable plan: PlanType
    ): ResponseEntity<PlanResource> = ResponseEntity.ok(plan.toResource())

    private fun PlanType.toResource() = PlanResource(
        plan = this,
        segment = segment,
        monthlyPrice = monthlyPriceSoles,
        currency = CURRENCY,
        billingPeriodDays = billingPeriodDays,
        maxWaterPoints = maxWaterPoints,
        maxAuthorizedUsers = maxAuthorizedUsers,
        technicalVisitsIncluded = true,
        purchasable = purchasable
    )

    companion object {
        private const val CURRENCY = "PEN"
    }
}
