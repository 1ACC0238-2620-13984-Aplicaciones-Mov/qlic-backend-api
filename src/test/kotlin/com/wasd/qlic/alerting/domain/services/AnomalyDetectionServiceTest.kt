package com.wasd.qlic.alerting.domain.services

import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AnomalyDetectionServiceTest {

    private lateinit var anomalyDetectionService: AnomalyDetectionService

    @BeforeEach
    fun setUp() {
        anomalyDetectionService = AnomalyDetectionService()
    }

    @Test
    @DisplayName("US15 - Escenario 1: Flujo sostenido por encima del umbral genera alerta CRITICAL")
    fun shouldReturnCriticalAlertWhenSustainedFlowExceedsThresholdSignificantly() {
        // Given (Dado)
        val flow = BigDecimal("120.0")
        val threshold = BigDecimal("50.0")
        val isSustained = true

        // When (Cuando)
        val result = anomalyDetectionService.evaluateReading(flow, threshold, isSustained)

        // Then (Entonces)
        assertNotNull(result)
        assertEquals(UrgencyLevel.CRITICAL, result)
    }

    @Test
    @DisplayName("US15 - Escenario 2: Lectura aislada por encima del umbral no genera alerta")
    fun shouldReturnNullWhenFlowExceedsThresholdButIsNotSustained() {
        // Given (Dado)
        val flow = BigDecimal("120.0")
        val threshold = BigDecimal("50.0")
        val isSustained = false

        // When (Cuando)
        val result = anomalyDetectionService.evaluateReading(flow, threshold, isSustained)

        // Then (Entonces)
        assertNull(result)
    }

    @Test
    @DisplayName("US15 - Consumo dentro del umbral normal no genera alerta")
    fun shouldReturnNullWhenFlowIsWithinThreshold() {
        // Given (Dado)
        val flow = BigDecimal("30.0")
        val threshold = BigDecimal("50.0")
        val isSustained = true

        // When (Cuando)
        val result = anomalyDetectionService.evaluateReading(flow, threshold, isSustained)

        // Then (Entonces)
        assertNull(result)
    }
}