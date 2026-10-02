package com.wasd.qlic.alerting.application.handlers

import com.wasd.qlic.alerting.application.commands.EvaluateConsumptionReadingCommand
import com.wasd.qlic.alerting.domain.model.LeakAlert
import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import com.wasd.qlic.alerting.domain.repositories.LeakAlertRepository
import com.wasd.qlic.alerting.domain.services.AnomalyDetectionService
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class EvaluateConsumptionReadingCommandHandler(
    private val anomalyDetectionService: AnomalyDetectionService,
    private val leakAlertRepository: LeakAlertRepository
) {
    fun handle(command: EvaluateConsumptionReadingCommand): LeakAlert? {
        val detectedUrgency = anomalyDetectionService.evaluateReading(
            command.currentFlowLitersPerHour,
            command.thresholdLimit,
            command.isSustained
        ) ?: return null

        val existingActive = leakAlertRepository.findActiveByWaterPointId(command.waterPointId)

        if (existingActive != null) {
            val additionalLiters = command.currentFlowLitersPerHour.multiply(BigDecimal("0.5"))
            val additionalSoles = additionalLiters.multiply(BigDecimal("0.005"))
            existingActive.escalate(detectedUrgency, additionalLiters, additionalSoles)
            return leakAlertRepository.save(existingActive)
        }

        val estimatedVolume = command.currentFlowLitersPerHour.multiply(BigDecimal("1.5"))
        val estimatedCost = estimatedVolume.multiply(BigDecimal("0.005"))

        val action = when (detectedUrgency) {
            UrgencyLevel.CRITICAL -> "Cierre la llave de paso general inmediatamente y verifique roturas en tuberia empotrada."
            UrgencyLevel.MODERATE -> "Inspeccione griferias, empaques y boyas de sanitarios para descartar goteo continuo."
            UrgencyLevel.INFORMATIONAL -> "Revise consumos inusuales fuera del horario regular de la vivienda o local."
        }

        val newAlert = LeakAlert(
            accountId = command.accountId,
            waterPointId = command.waterPointId,
            urgency = detectedUrgency,
            estimatedVolumeLiters = estimatedVolume,
            estimatedCostSoles = estimatedCost,
            recommendedAction = action
        )

        return leakAlertRepository.save(newAlert)
    }
}