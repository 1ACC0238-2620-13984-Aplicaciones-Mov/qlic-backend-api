package com.wasd.qlic.monitoring.interfaces.rest

import org.junit.jupiter.api.Assertions.assertNotEquals
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** Monitoring dashboard: live consumption, flow, pressure and device status from the readings. */
@SpringBootTest
class MonitoringDashboardIntegrationTest {

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
    @DisplayName("A reading with flow, pressure and battery is reflected in the dashboard and in the device status")
    fun dashboardReflectsTelemetry() {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Carla Ríos","email":"it.dashboard@example.com","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated)
        val auth = "Bearer " + field(
            mockMvc.perform(
                post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                    .content("""{"email":"it.dashboard@example.com","password":"Qlic2026!"}""")
            ).andExpect(status().isOk).andReturn().response.contentAsString,
            "token"
        )
        mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", auth)).andExpect(status().isCreated)
        val location = create(auth, "/api/v1/locations", """{"name":"Bodega Ríos"}""")
        val waterPoint = create(auth, "/api/v1/water-points", """{"locationId":"$location","name":"Línea principal","type":"MAIN_LINE"}""")
        val device = create(auth, "/api/v1/devices", """{"qrCodePayload":"QR-DASH-01","serialNumber":"QLIC-DASH-01","model":"QlicSense"}""")
        mockMvc.perform(
            patch("/api/v1/devices/$device/water-point").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"waterPointId":"$waterPoint"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"deviceId":"$device","volumeLiters":30,"flowRateLitersPerMinute":150,"pressureBar":2.2,"batteryPercentage":150}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.batteryPercentage").exists())

        mockMvc.perform(
            post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"deviceId":"$device","volumeLiters":30,"flowRateLitersPerMinute":7.5,"pressureBar":2.2,"batteryPercentage":10}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.flowRateLitersPerMinute").value(7.5))

        mockMvc.perform(get("/api/v1/monitoring/dashboard").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.todayLiters").value(30.0))
            .andExpect(jsonPath("$.currentFlowLitersPerMinute").value(7.5))
            .andExpect(jsonPath("$.averagePressureBar").value(2.2))
            .andExpect(jsonPath("$.devices.total").value(1))
            .andExpect(jsonPath("$.devices.lowBattery").value(1))
            .andExpect(jsonPath("$.waterPoints[0].name").value("Línea principal"))
            .andExpect(jsonPath("$.waterPoints[0].locationName").value("Bodega Ríos"))
            .andExpect(jsonPath("$.waterPoints[0].lastPressureBar").value(2.2))

        mockMvc.perform(get("/api/v1/devices/$device").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("LOW_BATTERY"))
            .andExpect(jsonPath("$.batteryPercentage").value(10))

        val first = mockMvc.perform(get("/api/v1/monitoring/dashboard").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(header().exists("ETag"))
            .andExpect(header().exists("Last-Modified"))
            .andExpect(jsonPath("$.lastReadingAt").exists())
            .andReturn().response
        val eTag = first.getHeader("ETag")!!

        mockMvc.perform(get("/api/v1/monitoring/dashboard").header("Authorization", auth).header("If-None-Match", eTag))
            .andExpect(status().isNotModified)
            .andExpect(header().string("ETag", eTag))

        mockMvc.perform(
            post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"deviceId":"$device","volumeLiters":12,"flowRateLitersPerMinute":6.0,"pressureBar":2.1,"batteryPercentage":80}""")
        ).andExpect(status().isCreated)

        val changed = mockMvc.perform(get("/api/v1/monitoring/dashboard").header("Authorization", auth).header("If-None-Match", eTag))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.todayLiters").value(42.0))
            .andReturn().response
        assertNotEquals(eTag, changed.getHeader("ETag"))
    }
}
