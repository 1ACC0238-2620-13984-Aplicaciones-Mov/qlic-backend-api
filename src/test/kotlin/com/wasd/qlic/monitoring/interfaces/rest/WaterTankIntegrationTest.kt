package com.wasd.qlic.monitoring.interfaces.rest

import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** US28, US29, US30: tank level, low level alert and replenishment prediction through the API. */
@SpringBootTest
class WaterTankIntegrationTest {

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

    private fun bearerFor(email: String): String {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Elena Quispe","email":"$email","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated)
        return "Bearer " + field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"$email","password":"Qlic2026!"}""")
            ).andExpect(status().isOk).andReturn().response.contentAsString,
            "token"
        )
    }

    private fun level(auth: String, tank: String, level: String, at: String) = mockMvc.perform(
        post("/api/v1/tanks/$tank/levels").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
            .content("""{"levelPercentage":$level,"recordedAt":"$at"}""")
    )

    @Test
    @DisplayName("Tank level, single low level alert and replenishment prediction")
    fun tankLifecycle() {
        val auth = bearerFor("it.tanks@example.com")
        val location = create(auth, "/api/v1/locations", """{"name":"Casa Quispe"}""")
        val tank = create(auth, "/api/v1/tanks", """{"locationId":"$location","name":"Tanque elevado","capacityLiters":1100}""")

        // US30 - Scenario 2: no history yet
        mockMvc.perform(get("/api/v1/tanks/$tank").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("NO_DATA"))
            .andExpect(jsonPath("$.criticalLevelPercentage").value(20.0))
            .andExpect(jsonPath("$.prediction.available").value(false))

        level(auth, tank, "100", "2026-11-18T08:00:00").andExpect(status().isCreated)
        level(auth, tank, "85", "2026-11-18T20:00:00").andExpect(status().isCreated)
        level(auth, tank, "70", "2026-11-19T08:00:00")
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.lowLevelAlertRaised").value(false))

        // US28 - Scenario 1 and US30 - Scenario 1
        mockMvc.perform(get("/api/v1/tanks/$tank").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentLevelPercentage").value(70.0))
            .andExpect(jsonPath("$.availableLiters").value(770.0))
            .andExpect(jsonPath("$.lastReadingAt").value(startsWith("2026-11-19T08:00")))
            .andExpect(jsonPath("$.locationName").value("Casa Quispe"))
            .andExpect(jsonPath("$.prediction.available").value(true))
            .andExpect(jsonPath("$.prediction.hoursToCritical").value(40.0))
            .andExpect(jsonPath("$.prediction.predictedCriticalAt").value(startsWith("2026-11-21T00:00")))

        // US29 - Scenario 1: the alert is raised once when crossing the critical level
        level(auth, tank, "18", "2026-11-20T10:00:00")
            .andExpect(jsonPath("$.lowLevelAlertRaised").value(true))
            .andExpect(jsonPath("$.tank.status").value("LOW"))
        level(auth, tank, "12", "2026-11-20T14:00:00")
            .andExpect(jsonPath("$.lowLevelAlertRaised").value(false))

        mockMvc.perform(
            get("/api/v1/tanks/$tank/levels").header("Authorization", auth).param("from", "2026-11-18").param("to", "2026-11-20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(5))

        mockMvc.perform(delete("/api/v1/locations/$location").header("Authorization", auth))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("monitoring.location.has-tanks"))

        level(auth, tank, "130", "2026-11-20T15:00:00")
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("A tank cannot be read by another account")
    fun tanksAreIsolatedByAccount() {
        val owner = bearerFor("it.tanks.owner@example.com")
        val other = bearerFor("it.tanks.other@example.com")
        val location = create(owner, "/api/v1/locations", """{"name":"Casa"}""")
        val tank = create(owner, "/api/v1/tanks", """{"locationId":"$location","name":"Cisterna","capacityLiters":2500}""")

        mockMvc.perform(get("/api/v1/tanks/$tank").header("Authorization", other))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("monitoring.tank.not-found"))
        level(other, tank, "50", "2026-11-20T10:00:00").andExpect(status().isNotFound)
    }
}
