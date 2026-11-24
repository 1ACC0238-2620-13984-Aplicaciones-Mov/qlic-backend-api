package com.wasd.qlic.support.interfaces.rest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

/** Integration test of the Support endpoints (US35, US36, US37, US38). */
@SpringBootTest
class SupportIntegrationTest {

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
    @DisplayName("US38 - Scenario 1: the current terms are public and indicate their update date")
    fun termsOfServiceArePublic() {
        mockMvc.perform(get("/api/v1/terms-of-service/current").header("Accept-Language", "es-419"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.language").value("es"))
            .andExpect(jsonPath("$.version").value("1.0"))
            .andExpect(jsonPath("$.updatedOn").exists())
    }

    @Test
    @DisplayName("US35, US36 and US37: register an incident, request a visit, assign a technician and follow it up")
    fun incidentLifecycle() {
        val signUp = mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Claudia Morales","email":"it.support@example.com","password":"Qlic2026!","acceptTerms":true}""")
        ).andReturn().response.contentAsString
        val accountId = field(signUp, "accountId")
        val token = field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"it.support@example.com","password":"Qlic2026!"}""")
            ).andReturn().response.contentAsString,
            "token"
        )
        val auth = "Bearer $token"

        mockMvc.perform(
            post("/api/v1/subscriptions/trial").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""{"accountId":"$accountId"}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/incidents").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"description":"Leak under the sink"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("support.incident.asset-required"))

        val created = mockMvc.perform(
            post("/api/v1/incidents").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"description":"Leak under the sink","waterPointId":"7c9e6679-7425-40de-944b-e07fc1f90ae7"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.state").value("PENDING"))
            .andReturn().response.contentAsString
        val incidentId = field(created, "id")

        val date = LocalDate.now().plusDays(2)
        mockMvc.perform(
            post("/api/v1/incidents/$incidentId/technical-visit").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"preferredDate":"$date","fromTime":"09:00","toTime":"13:00"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.state").value("VISIT_REQUESTED"))

        mockMvc.perform(
            post("/api/v1/incidents/$incidentId/technical-visit/assignment").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""{"scheduledAt":"${date}T10:00:00"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.technicalVisit.technicianName").exists())

        mockMvc.perform(get("/api/v1/incidents").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(incidentId))
            .andExpect(jsonPath("$[0].state").value("IN_PROGRESS"))
    }
}
