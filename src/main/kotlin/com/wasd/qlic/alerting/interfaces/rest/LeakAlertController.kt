package com.wasd.qlic.alerting.interfaces.rest

import com.wasd.qlic.alerting.application.commands.EvaluateConsumptionReadingCommand
import com.wasd.qlic.alerting.application.handlers.EvaluateConsumptionReadingCommandHandler
import com.wasd.qlic.alerting.application.handlers.GetAlertsByAccountQueryHandler
import com.wasd.qlic.alerting.application.queries.GetAlertsByAccountQuery
import com.wasd.qlic.alerting.interfaces.rest.resources.EvaluateReadingRequest
import com.wasd.qlic.alerting.interfaces.rest.resources.LeakAlertResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/alerts")
class LeakAlertController(
    private val evaluateHandler: EvaluateConsumptionReadingCommandHandler,
    private val getAlertsHandler: GetAlertsByAccountQueryHandler
) {

    // US15: Ingesta y evaluacion de lecturas de consumo
    @PostMapping("/evaluations")
    fun evaluateReading(@RequestBody request: EvaluateReadingRequest): ResponseEntity<Any> {
        val command = EvaluateConsumptionReadingCommand(
            accountId = request.accountId,
            waterPointId = request.waterPointId,
            currentFlowLitersPerHour = request.currentFlowLitersPerHour,
            thresholdLimit = request.thresholdLimit,
            isSustained = request.isSustained
        )

        val result = evaluateHandler.handle(command)
        return if (result != null) {
            val resource = LeakAlertResource(
                id = result.id,
                accountId = result.accountId,
                waterPointId = result.waterPointId,
                state = result.state,
                urgency = result.urgency,
                detectedAt = result.detectedAt,
                estimatedVolumeLiters = result.estimatedVolumeLiters,
                estimatedCostSoles = result.estimatedCostSoles,
                recommendedAction = result.recommendedAction
            )
            ResponseEntity.status(HttpStatus.CREATED).body(resource)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    // US16: Consulta de alertas por cuenta
    @GetMapping
    fun getAlertsByAccount(@RequestParam accountId: UUID): ResponseEntity<List<LeakAlertResource>> {
        val query = GetAlertsByAccountQuery(accountId)
        val alerts = getAlertsHandler.handle(query).map {
            LeakAlertResource(
                id = it.id,
                accountId = it.accountId,
                waterPointId = it.waterPointId,
                state = it.state,
                urgency = it.urgency,
                detectedAt = it.detectedAt,
                estimatedVolumeLiters = it.estimatedVolumeLiters,
                estimatedCostSoles = it.estimatedCostSoles,
                recommendedAction = it.recommendedAction
            )
        }
        return ResponseEntity.ok(alerts)
    }
}