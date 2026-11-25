package com.wasd.qlic.monitoring.interfaces.rest

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** US13: locations group the Water Points, the plan limits them and the readings can be queried per location. */
@SpringBootTest
class WaterPointIntegrationTest {

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

    private fun create(auth: String, path: String, json: String): String = field(
        mockMvc.perform(post(path).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated).andReturn().response.contentAsString,
        "id"
    )

    @Test
    @DisplayName("US13 - Scenario 1: the devices and readings of one location are returned separately")
    fun readingsAndDevicesByLocation() {
        val auth = bearerFor("Lucho Ramos", "it.locations.owner@example.com")
        mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", auth)).andExpect(status().isCreated)

        val miraflores = create(auth, "/api/v1/locations", """{"name":"Local Miraflores","address":"Av. Larco 345"}""")
        val surco = create(auth, "/api/v1/locations", """{"name":"Local Surco"}""")
        val kitchen = create(auth, "/api/v1/water-points", """{"locationId":"$miraflores","name":"Cocina","type":"KITCHEN"}""")
        val garden = create(auth, "/api/v1/water-points", """{"locationId":"$surco","name":"Jardín","type":"GARDEN"}""")

        mockMvc.perform(
            post("/api/v1/water-points").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("""{"locationId":"$surco","name":"Baño"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("monitoring.water-point.limit-reached"))

        listOf(Triple("QLIC-LOC-01", kitchen, "40"), Triple("QLIC-LOC-02", garden, "15")).forEach { (serial, waterPoint, liters) ->
            val deviceId = create(auth, "/api/v1/devices", """{"qrCodePayload":"QR-$serial","serialNumber":"$serial","model":"QlicSense"}""")
            mockMvc.perform(
                patch("/api/v1/devices/$deviceId/water-point").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"waterPointId":"$waterPoint"}""")
            ).andExpect(status().isOk)
            mockMvc.perform(
                post("/api/v1/consumption-readings").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                    .content("""{"deviceId":"$deviceId","volumeLiters":$liters,"recordedAt":"2026-11-10T09:00:00"}""")
            ).andExpect(status().isCreated)
        }

        mockMvc.perform(get("/api/v1/devices").header("Authorization", auth).param("locationId", miraflores))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].waterPointId").value(kitchen))

        mockMvc.perform(
            get("/api/v1/consumption-readings").header("Authorization", auth)
                .param("from", "2026-11-01").param("to", "2026-11-30").param("locationId", surco)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].waterPointId").value(garden))
            .andExpect(jsonPath("$[0].volumeLiters").value(15.0))

        mockMvc.perform(get("/api/v1/locations").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Local Miraflores"))
            .andExpect(jsonPath("$[0].waterPointsCount").value(1))

        mockMvc.perform(get("/api/v1/water-points/$kitchen").header("Authorization", auth))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.locationName").value("Local Miraflores"))
            .andExpect(jsonPath("$.devicesCount").value(1))

        mockMvc.perform(delete("/api/v1/locations/$miraflores").header("Authorization", auth))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("monitoring.location.has-water-points"))
    }

    @Test
    @DisplayName("A Water Point cannot be read or assigned by another account")
    fun waterPointsAreIsolatedByAccount() {
        val owner = bearerFor("Rosa Díaz", "it.locations.isolation.owner@example.com")
        val other = bearerFor("Raúl Soto", "it.locations.isolation.other@example.com")
        mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", owner)).andExpect(status().isCreated)
        val location = create(owner, "/api/v1/locations", """{"name":"Casa"}""")
        val waterPoint = create(owner, "/api/v1/water-points", """{"locationId":"$location","name":"Lavandería","type":"LAUNDRY"}""")
        val foreignDevice = create(other, "/api/v1/devices", """{"qrCodePayload":"QR-ISO-WP","serialNumber":"QLIC-ISO-WP","model":"QlicSense"}""")

        mockMvc.perform(get("/api/v1/water-points/$waterPoint").header("Authorization", other))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("monitoring.water-point.not-found"))

        mockMvc.perform(
            patch("/api/v1/devices/$foreignDevice/water-point").header("Authorization", other).contentType(MediaType.APPLICATION_JSON)
                .content("""{"waterPointId":"$waterPoint"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("monitoring.water-point.not-found"))
    }
}
