package com.wasd.qlic.alerting.interfaces.rest

import com.wasd.qlic.monitoring.application.services.MeterSimulator
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
import java.time.LocalDateTime
import kotlin.random.Random

/** US15: the readings of a meter are evaluated automatically and a sustained excess raises one leak alert. */
@SpringBootTest
class LeakAutoDetectionIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var meterSimulator: MeterSimulator

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

    /** Creates an account with a trial, a Water Point and a device assigned to it; returns the token and the device. */
    private fun accountWithMeter(email: String, serial: String): Pair<String, String> {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Rosa Huamán","email":"$email","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated)
        val auth = "Bearer " + field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"$email","password":"Qlic2026!"}""")
            ).andExpect(status().isOk).andReturn().response.contentAsString,
            "token"
        )
        mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", auth)).andExpect(status().isCreated)
        val location = create(auth, "/api/v1/locations", """{"name":"Casa Huamán"}""")
        val waterPoint = create(auth, "/api/v1/water-points", """{"locationId":"$location","name":"Baño","type":"BATHROOM"}""")
        val device = create(auth, "/api/v1/devices", """{"qrCodePayload":"QR-$serial","serialNumber":"$serial","model":"QlicSense"}""")
        mockMvc.perform(
            patch("/api/v1/devices/$device/water-point").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"waterPointId":"$waterPoint"}""")
        ).andExpect(status().isOk)
        return auth to device
    }

    private fun reading(auth: String, device: String, flowPerMinute: String, recordedAt: LocalDateTime) {
        mockMvc.perform(
            post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"deviceId":"$device","volumeLiters":20,"flowRateLitersPerMinute":$flowPerMinute,"recordedAt":"$recordedAt"}""")
        ).andExpect(status().isCreated)
    }

    @Test
    @DisplayName("An isolated peak is ignored; a sustained flow above the threshold raises a single critical alert")
    fun sustainedFlowRaisesOneAlert() {
        val (auth, device) = accountWithMeter("it.autoleak@example.com", "QLIC-AUTO-01")
        val now = LocalDateTime.now().withNano(0)

        reading(auth, device, "6", now.minusMinutes(50))
        mockMvc.perform(get("/api/v1/alerts").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))

        reading(auth, device, "1", now.minusMinutes(20))
        reading(auth, device, "6", now.minusMinutes(10))
        mockMvc.perform(get("/api/v1/alerts").header("Authorization", auth))
            .andExpect(jsonPath("$.length()").value(0))

        reading(auth, device, "6", now.minusMinutes(5))
        reading(auth, device, "6.5", now)
        mockMvc.perform(get("/api/v1/alerts").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].state").value("ACTIVE"))
            .andExpect(jsonPath("$[0].urgency").value("CRITICAL"))
    }

    @Test
    @DisplayName("A device in LEAK simulation sends readings that end in a leak alert; it needs a Water Point")
    fun simulatedLeakRaisesAlert() {
        val (auth, device) = accountWithMeter("it.simulation@example.com", "QLIC-SIM-01")

        mockMvc.perform(
            put("/api/v1/devices/$device/simulation").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":true,"scenario":"LEAK"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.simulationScenario").value("LEAK"))

        val now = LocalDateTime.now().withNano(0)
        meterSimulator.tick(now.minusMinutes(4), Random(7))
        meterSimulator.tick(now, Random(8))

        mockMvc.perform(get("/api/v1/alerts").header("Authorization", auth))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].urgency").value("CRITICAL"))

        mockMvc.perform(
            put("/api/v1/devices/$device/simulation").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.simulationScenario").doesNotExist())

        val unassigned = create(auth, "/api/v1/devices", """{"qrCodePayload":"QR-SIM-02","serialNumber":"QLIC-SIM-02","model":"QlicSense"}""")
        mockMvc.perform(
            put("/api/v1/devices/$unassigned/simulation").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":true,"scenario":"NORMAL"}""")
        ).andExpect(status().isConflict)
    }
}
