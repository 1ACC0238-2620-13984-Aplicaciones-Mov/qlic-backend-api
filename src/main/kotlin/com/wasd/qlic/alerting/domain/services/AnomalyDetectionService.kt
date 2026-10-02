package com.wasd.qlic.alerting.domain.services

import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class AnomalyDetectionService {

    // Regla de contorno US15: solo lecturas sostenidas generan alerta
    fun evaluateReading(
        currentFlowLitersPerHour: BigDecimal,
        thresholdLimit: BigDecimal,
        isSustained: Boolean
    ): UrgencyLevel? {
        if (!isSustained) {
            return null // Variación aislada: se descarta
        }

        val excess = currentFlowLitersPerHour.subtract(thresholdLimit)
        if (excess <= BigDecimal.ZERO) {
            return null
        }

        val percentageOver = excess.divide(thresholdLimit, 2, java.math.RoundingMode.HALF_UP).toDouble()

        return when {
            percentageOver >= 0.80 -> UrgencyLevel.CRITICAL
            percentageOver >= 0.30 -> UrgencyLevel.MODERATE
            else -> UrgencyLevel.INFORMATIONAL
        }
    }
}