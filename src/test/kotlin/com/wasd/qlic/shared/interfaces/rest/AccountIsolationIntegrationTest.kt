package com.wasd.qlic.shared.interfaces.rest

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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * TS01: Device Monitoring, Alerting and Subscriptions take the account from the access token, so a
 * subscriber can neither read nor modify the resources of another account.
 */
@SpringBootTest
class AccountIsolationIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun field(json: String, name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(json)!!.groupValues[1]

    private fun bearerFor(fullName: String, email: String): String {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"$fullName","email":"$email","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated)
        val body = mockMvc.perform(
            post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"Qlic2026!"}""")
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return "Bearer " + field(body, "token")
    }

    @Test
    @DisplayName("Devices: another account does not see, assign or re-register the devices of the owner")
    fun devicesAreIsolatedByAccount() {
        val owner = bearerFor("Rosa Huamán", "it.isolation.devices.owner@example.com")
        val other = bearerFor("Mario Salas", "it.isolation.devices.other@example.com")

        val deviceId = field(
            mockMvc.perform(
                post("/api/v1/devices").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"qrCodePayload":"QLIC-QR-ISO-01","serialNumber":"QLIC-ISO-01","model":"QlicSense"}""")
            ).andExpect(status().isCreated).andReturn().response.contentAsString,
            "id"
        )

        mockMvc.perform(get("/api/v1/devices").header("Authorization", other))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        mockMvc.perform(
            patch("/api/v1/devices/$deviceId/water-point").header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
                .content("""{"waterPointId":"3fa85f64-5717-4562-b3fc-2c963f66afa6"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("monitoring.device.not-found"))

        mockMvc.perform(
            post("/api/v1/devices").header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
                .content("""{"qrCodePayload":"QLIC-QR-ISO-01","serialNumber":"QLIC-ISO-01","model":"QlicSense"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("monitoring.device.already-registered"))
    }

    @Test
    @DisplayName("Alerts: another account cannot read or toggle the alert recipients of the owner")
    fun alertRecipientsAreIsolatedByAccount() {
        val owner = bearerFor("Lucía Torres", "it.isolation.alerts.owner@example.com")
        val other = bearerFor("Carlos Vega", "it.isolation.alerts.other@example.com")

        val recipientId = field(
            mockMvc.perform(
                post("/api/v1/alerts/recipients").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"fullName":"Lucía Torres","email":"lucia@example.com","devicePushToken":"push-token-01"}""")
            ).andExpect(status().isCreated).andReturn().response.contentAsString,
            "id"
        )

        mockMvc.perform(get("/api/v1/alerts/recipients").header("Authorization", other))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        mockMvc.perform(
            patch("/api/v1/alerts/recipients/$recipientId/toggle").header("Authorization", other)
                .contentType(MediaType.APPLICATION_JSON).content("""{"enabled":false}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("alerting.recipient.not-found"))
    }

    @Test
    @DisplayName("Subscriptions: another account cannot cancel the subscription of the owner")
    fun subscriptionsAreIsolatedByAccount() {
        val owner = bearerFor("Ana Quispe", "it.isolation.subscriptions.owner@example.com")
        val other = bearerFor("Jorge Ríos", "it.isolation.subscriptions.other@example.com")

        val subscriptionId = field(
            mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", owner))
                .andExpect(status().isCreated).andReturn().response.contentAsString,
            "id"
        )

        mockMvc.perform(post("/api/v1/subscriptions/$subscriptionId/cancel").header("Authorization", other))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("subscriptions.subscription.not-found"))

        mockMvc.perform(get("/api/v1/subscriptions/current").header("Authorization", owner))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("TRIAL_ACTIVE"))
    }
}
