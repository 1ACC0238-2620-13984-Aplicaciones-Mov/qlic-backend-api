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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * Integration test of the IAM endpoints and the token-based security (US05, US06, TS01),
 * running the full Spring context with the in-memory database.
 */
@SpringBootTest
class AuthenticationIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    private fun signUpBody(email: String) =
        """{"fullName":"Claudia Morales","email":"$email","password":"Qlic2026!","phone":"+51987654321","acceptTerms":true}"""

    @Test
    @DisplayName("US05 - Scenarios 1 and 2: sign-up returns 201 and a duplicated email returns 409")
    fun signUpCreatesAccountAndRejectsDuplicates() {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up")
                .contentType(MediaType.APPLICATION_JSON).content(signUpBody("it.signup@example.com"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").value("it.signup@example.com"))
            .andExpect(jsonPath("$.accountId").exists())

        mockMvc.perform(
            post("/api/v1/authentication/sign-up")
                .contentType(MediaType.APPLICATION_JSON).content(signUpBody("it.signup@example.com"))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.detail").value("The email it.signup@example.com is already registered."))
    }

    @Test
    @DisplayName("US06 - Scenarios 1 and 2: sign-in returns a Bearer token and wrong credentials return 401")
    fun signInIssuesTokenAndRejectsWrongCredentials() {
        mockMvc.perform(
            post("/api/v1/authentication/sign-up")
                .contentType(MediaType.APPLICATION_JSON).content(signUpBody("it.signin@example.com"))
        )

        mockMvc.perform(
            post("/api/v1/authentication/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"it.signin@example.com","password":"Qlic2026!"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.token").isNotEmpty)

        mockMvc.perform(
            post("/api/v1/authentication/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"it.signin@example.com","password":"wrong-password"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("TS01 - Scenario 3: a protected resource without token returns 401")
    fun protectedResourceWithoutTokenReturnsUnauthorized() {
        mockMvc.perform(get("/api/v1/devices").param("accountId", "c41d8e7a-2f6b-4a9d-8c3e-5b1a7f9d2e61"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("i18n: Accept-Language es-419 returns the error message in Latin American Spanish")
    fun errorMessagesAreLocalized() {
        mockMvc.perform(
            post("/api/v1/authentication/sign-in")
                .header("Accept-Language", "es-419")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"nobody@example.com","password":"Qlic2026!"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.detail").value("Las credenciales no son válidas."))
    }
}
