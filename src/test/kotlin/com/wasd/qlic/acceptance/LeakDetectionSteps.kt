package com.wasd.qlic.acceptance

import com.wasd.qlic.alerting.domain.model.UrgencyLevel
import com.wasd.qlic.alerting.domain.services.AnomalyDetectionService
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import java.math.BigDecimal

/** Steps of deteccion_automatica_de_fugas.feature, on the same domain rules used by the reading ingestion. */
class LeakDetectionSteps {

    private val anomalyDetectionService = AnomalyDetectionService()
    private lateinit var threshold: BigDecimal
    private var previousFlow: BigDecimal? = null
    private var result: UrgencyLevel? = null

    @Given("un punto de agua con un umbral de {int} litros por hora")
    fun waterPointWithThreshold(limit: Int) {
        threshold = BigDecimal(limit)
    }

    @Given("que la lectura anterior reportó {int} litros por hora")
    fun previousReading(flow: Int) {
        previousFlow = BigDecimal(flow)
    }

    @When("el medidor reporta un caudal de {int} litros por hora sin una lectura previa reciente")
    fun readingWithoutPrevious(flow: Int) {
        previousFlow = null
        evaluate(flow)
    }

    @When("el medidor reporta un caudal de {int} litros por hora")
    fun reading(flow: Int) {
        evaluate(flow)
    }

    @Then("no se genera ninguna alerta")
    fun noAlert() {
        assertNull(result)
    }

    @Then("se genera una alerta con urgencia {string}")
    fun alertWithUrgency(urgency: String) {
        assertEquals(UrgencyLevel.valueOf(urgency), result)
    }

    private fun evaluate(flow: Int) {
        val sustained = anomalyDetectionService.isSustained(previousFlow, threshold)
        result = anomalyDetectionService.evaluateReading(BigDecimal(flow), threshold, sustained)
    }
}
