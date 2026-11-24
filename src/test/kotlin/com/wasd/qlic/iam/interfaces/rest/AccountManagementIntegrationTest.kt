package com.wasd.qlic.iam.interfaces.rest

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * Integration test of the account management endpoints of IAM (US07, US08, US09) with the
 * full Spring context, the JWT security and the in-memory database.
 */
@SpringBootTest
class AccountManagementIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun field(json: String, name: String): String =
        Regex("\"$name\"\\s*:\\s*\"([^\"]+)\"").find(json)!!.groupValues[1]

    private fun signUp(fullName: String, email: String): String =
        mockMvc.perform(
            post("/api/v1/authentication/sign-up").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"$fullName","email":"$email","password":"Qlic2026!","acceptTerms":true}""")
        ).andExpect(status().isCreated).andReturn().response.contentAsString

    private fun signIn(email: String, password: String = "Qlic2026!", accountId: String? = null): String {
        val account = accountId?.let { ",\"accountId\":\"$it\"" } ?: ""
        val body = mockMvc.perform(
            post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"$account}""")
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return field(body, "token")
    }

    @Test
    @DisplayName("US07 - Scenarios 1 and 2: the reset token sets a new password and cannot be reused")
    fun passwordResetFlow() {
        signUp("Rosa Huamán", "it.reset@example.com")

        val requested = mockMvc.perform(
            post("/api/v1/authentication/password-reset/request").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"it.reset@example.com"}""")
        ).andExpect(status().isAccepted).andReturn().response.contentAsString
        val token = field(requested, "resetToken")

        mockMvc.perform(
            post("/api/v1/authentication/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token","newPassword":"NuevaClave2026!"}""")
        ).andExpect(status().isNoContent)

        signIn("it.reset@example.com", "NuevaClave2026!")

        mockMvc.perform(
            post("/api/v1/authentication/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token","newPassword":"OtraClave2026!"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("iam.reset-token.invalid"))
    }

    @Test
    @DisplayName("US09 - Scenarios 1 and 2: the profile is updated and an empty full name is rejected")
    fun profileUpdate() {
        signUp("Luis Ramos", "it.profile@example.com")
        val token = signIn("it.profile@example.com")

        mockMvc.perform(
            put("/api/v1/accounts/me").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Luis Ramos Díaz","phone":"+51911111111","accountName":"Restaurante Don Lucho"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountName").value("Restaurante Don Lucho"))

        mockMvc.perform(get("/api/v1/accounts/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fullName").value("Luis Ramos Díaz"))

        mockMvc.perform(
            put("/api/v1/accounts/me").header("Authorization", "Bearer $token").contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.fullName").exists())
    }

    @Test
    @DisplayName("US08 - Scenarios 1 and 2: an invited VIEWER can consult but cannot modify the devices")
    fun sharedAccessWithViewerPermission() {
        val ownerAccountId = field(signUp("Claudia Morales", "it.owner@example.com"), "accountId")
        val ownerToken = signIn("it.owner@example.com")
        signUp("Jorge Quispe", "it.viewer@example.com")

        mockMvc.perform(
            post("/api/v1/subscriptions/trial").header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON).content("""{"accountId":"$ownerAccountId"}""")
        ).andExpect(status().isCreated)

        val invitation = mockMvc.perform(
            post("/api/v1/invitations").header("Authorization", "Bearer $ownerToken").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"it.viewer@example.com","permission":"VIEWER"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.state").value("PENDING"))
            .andReturn().response.contentAsString

        val viewerOwnToken = signIn("it.viewer@example.com")
        mockMvc.perform(
            post("/api/v1/invitations/${field(invitation, "id")}/acceptance").header("Authorization", "Bearer $viewerOwnToken")
        ).andExpect(status().isCreated)

        val viewerToken = signIn("it.viewer@example.com", accountId = ownerAccountId)

        mockMvc.perform(get("/api/v1/devices").param("accountId", ownerAccountId).header("Authorization", "Bearer $viewerToken"))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/v1/devices").header("Authorization", "Bearer $viewerToken").contentType(MediaType.APPLICATION_JSON)
                .content("""{"qrCodePayload":"QLIC-QR-IT-01","serialNumber":"QLIC-IT-01","model":"QlicSense","accountId":"$ownerAccountId"}""")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("iam.permission.denied"))
    }
}
