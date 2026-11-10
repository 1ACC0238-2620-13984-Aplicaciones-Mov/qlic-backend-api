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

/** Integration test of the off-hours consumption report (US24) with readings ingested through the API. */
@SpringBootTest
class ConsumptionReportIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun field(json: String, name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(json)!!.groupValues[1]

    @Test
    @DisplayName("US24 - Scenario 1: the report separates the consumption registered while the premises is closed")
    fun offHoursReport() {
        val accountId = field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"fullName":"Pedro Lazo","email":"it.offhours@example.com","password":"Qlic2026!","acceptTerms":true}""")
            ).andReturn().response.contentAsString,
            "accountId"
        )
        val auth = "Bearer " + field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"it.offhours@example.com","password":"Qlic2026!"}""")
            ).andReturn().response.contentAsString,
            "token"
        )

        val deviceId = field(
            mockMvc.perform(
                post("/api/v1/devices").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"qrCodePayload":"QLIC-QR-OFF-01","serialNumber":"QLIC-OFF-01","model":"QlicSense","accountId":"$accountId"}""")
            ).andExpect(status().isCreated).andReturn().response.contentAsString,
            "id"
        )
        mockMvc.perform(
            patch("/api/v1/devices/$deviceId/water-point").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"waterPointId":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/consumption/reports/activity-hours").header("Authorization", auth)
            .param("from", "2026-11-02").param("to", "2026-11-08"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scheduleDefined").value(false))

        mockMvc.perform(
            put("/api/v1/consumption/activity-schedule").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"openingTime":"08:00","closingTime":"20:00","activeDays":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY"]}""")
        ).andExpect(status().isOk)

        listOf("75" to "2026-11-02T10:00:00", "25" to "2026-11-02T23:00:00").forEach { (liters, at) ->
            mockMvc.perform(
                post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"deviceId":"$deviceId","volumeLiters":$liters,"recordedAt":"$at"}""")
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(get("/api/v1/consumption/reports/activity-hours").header("Authorization", auth)
            .param("from", "2026-11-02").param("to", "2026-11-08"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scheduleDefined").value(true))
            .andExpect(jsonPath("$.totalLiters").value(100.0))
            .andExpect(jsonPath("$.outsideActivityLiters").value(25.0))
            .andExpect(jsonPath("$.outsideActivityPercentage").value(25.0))
    }
}
