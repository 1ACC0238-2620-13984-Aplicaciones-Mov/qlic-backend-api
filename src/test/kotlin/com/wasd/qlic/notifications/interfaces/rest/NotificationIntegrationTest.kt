package com.wasd.qlic.notifications.interfaces.rest

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

/**
 * TS06 through the API. Without Firebase credentials (tests and development) the attempts are recorded as
 * DISABLED, which proves the flow from the domain event to the delivery record.
 */
@SpringBootTest
class NotificationIntegrationTest {

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
    @DisplayName("A registered device receives the test push and the low level alert of a tank")
    fun pushFlow() {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Pedro Lazo","email":"it.push@example.com","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated)
        val auth = "Bearer " + field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"it.push@example.com","password":"Qlic2026!"}""")
            ).andExpect(status().isOk).andReturn().response.contentAsString,
            "token"
        )

        mockMvc.perform(
            post("/api/v1/notifications/device-tokens").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"it-fcm-token-0001-abcdef12","platform":"ANDROID"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.maskedToken").value("…abcdef12"))
            .andExpect(jsonPath("$.active").value(true))

        mockMvc.perform(post("/api/v1/notifications/test").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].category").value("TEST"))
            .andExpect(jsonPath("$[0].status").value("DISABLED"))

        val location = create(auth, "/api/v1/locations", """{"name":"Casa Lazo"}""")
        val tank = create(auth, "/api/v1/tanks", """{"locationId":"$location","name":"Cisterna","capacityLiters":2000}""")
        mockMvc.perform(
            post("/api/v1/tanks/$tank/levels").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"levelPercentage":15,"recordedAt":"2026-11-20T09:00:00"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.lowLevelAlertRaised").value(true))

        mockMvc.perform(get("/api/v1/notifications/deliveries").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.category == 'TANK_LEVEL_LOW')].title").value("Nivel crítico en Cisterna"))

        mockMvc.perform(
            delete("/api/v1/notifications/device-tokens").header("Authorization", auth).param("token", "it-fcm-token-0001-abcdef12")
        ).andExpect(status().isNoContent)
        mockMvc.perform(get("/api/v1/notifications/device-tokens").header("Authorization", auth))
            .andExpect(jsonPath("$.length()").value(0))
    }
}
