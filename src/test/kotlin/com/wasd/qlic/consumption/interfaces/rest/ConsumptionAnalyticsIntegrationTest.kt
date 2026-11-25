package com.wasd.qlic.consumption.interfaces.rest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

/** US21, US22, US23, US25, US26, US27: consumption reports built from the readings of the devices. */
@SpringBootTest
class ConsumptionAnalyticsIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun field(json: String, name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(json)!!.groupValues[1]

    private fun create(auth: String, path: String, json: String): String = field(
        mockMvc.perform(post(path).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated).andReturn().response.contentAsString,
        "id"
    )

    @Test
    @DisplayName("Period report, comparison, cost, recommendations, sharing and goal from the readings")
    fun consumptionAnalytics() {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Mario Paz","email":"it.analytics@example.com","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated)
        val auth = "Bearer " + field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"it.analytics@example.com","password":"Qlic2026!"}""")
            ).andExpect(status().isOk).andReturn().response.contentAsString,
            "token"
        )
        mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", auth)).andExpect(status().isCreated)
        val location = create(auth, "/api/v1/locations", """{"name":"Casa Paz"}""")
        val kitchen = create(auth, "/api/v1/water-points", """{"locationId":"$location","name":"Cocina","type":"KITCHEN"}""")
        val device = create(auth, "/api/v1/devices", """{"qrCodePayload":"QR-ANA-01","serialNumber":"QLIC-ANA-01","model":"QlicSense"}""")
        mockMvc.perform(
            patch("/api/v1/devices/$device/water-point").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"waterPointId":"$kitchen"}""")
        ).andExpect(status().isOk)

        val october = (1..31).map { LocalDate.of(2026, 10, it) to 100 }
        val november = (1..10).map { LocalDate.of(2026, 11, it) to 120 }
        (october + november).forEach { (date, liters) ->
            mockMvc.perform(
                post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"deviceId":"$device","volumeLiters":$liters,"recordedAt":"${date}T10:00:00"}""")
            ).andExpect(status().isCreated)
        }

        // US22 - Scenario 1: 1200 L against 1000 L of the previous 10 days
        mockMvc.perform(
            get("/api/v1/consumption/reports/comparison").header("Authorization", auth)
                .param("from", "2026-11-01").param("to", "2026-11-10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.comparable").value(true))
            .andExpect(jsonPath("$.variationLiters").value(200.0))
            .andExpect(jsonPath("$.variationPercentage").value(20.0))
            .andExpect(jsonPath("$.trend").value("INCREASING"))

        // US22 - Scenario 2: September has no readings
        mockMvc.perform(
            get("/api/v1/consumption/reports/comparison").header("Authorization", auth)
                .param("from", "2026-10-01").param("to", "2026-10-31")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.comparable").value(false))

        // US27 - Scenario 1: 10 % less than October (3100 L)
        mockMvc.perform(
            put("/api/v1/consumption/goals/2026-11").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"reductionPercentage":10}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.baselineLiters").value(3100.0))
            .andExpect(jsonPath("$.targetLiters").value(2790.0))

        mockMvc.perform(
            put("/api/v1/consumption/goals/2026-10").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"reductionPercentage":10}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("consumption.goal.baseline-unavailable"))

        // US21, US23, US25: report of the first 10 days of November with the domestic reference tariff
        mockMvc.perform(
            get("/api/v1/consumption/reports/period").header("Authorization", auth)
                .param("from", "2026-11-01").param("to", "2026-11-10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasData").value(true))
            .andExpect(jsonPath("$.totalLiters").value(1200.0))
            .andExpect(jsonPath("$.estimatedCost.amount").value(6.0))
            .andExpect(jsonPath("$.estimatedCost.estimate").value(true))
            .andExpect(jsonPath("$.tariff.category").value("DOMESTIC"))
            .andExpect(jsonPath("$.byWaterPoint[0].waterPointName").value("Cocina"))
            .andExpect(jsonPath("$.byWaterPoint[0].percentage").value(100.0))
            .andExpect(jsonPath("$.recommendationsAvailable").value(true))
            .andExpect(jsonPath("$.recommendations[0].increasePercentage").value(20.0))
            .andExpect(jsonPath("$.goal.targetLiters").value(2790.0))

        // US23: own price per cubic meter
        mockMvc.perform(
            put("/api/v1/consumption/tariff").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"category":"COMMERCIAL","customPricePerCubicMeter":6.00}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.custom").value(true))
        mockMvc.perform(
            get("/api/v1/consumption/reports/period").header("Authorization", auth)
                .param("from", "2026-11-01").param("to", "2026-11-10")
        ).andExpect(jsonPath("$.estimatedCost.amount").value(7.2))

        // US26: summary ready to share, localized
        mockMvc.perform(
            get("/api/v1/consumption/reports/period/share").header("Authorization", auth).header("Accept-Language", "es-419")
                .param("from", "2026-11-01").param("to", "2026-11-10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Reporte de consumo Qlic del 2026-11-01 al 2026-11-10"))

        // US21 - Scenario 2: period without readings
        mockMvc.perform(
            get("/api/v1/consumption/reports/period").header("Authorization", auth)
                .param("from", "2026-09-01").param("to", "2026-09-30")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.hasData").value(false))
            .andExpect(jsonPath("$.message").exists())
    }
}
