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
 * US08: the member application. A person invited by the account holder signs in, sees the shared account in
 * sharedAccounts and opens a session over it without typing the password again.
 */
@SpringBootTest
class MemberSessionIntegrationTest {

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

    private fun signIn(email: String): String =
        mockMvc.perform(
            post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"Qlic2026!"}""")
        ).andExpect(status().isOk).andReturn().response.contentAsString

    @Test
    @DisplayName("An invited member opens a session over the shared account and keeps read-only access")
    fun memberOpensSessionOverSharedAccount() {
        val ownerAccountId = field(signUp("Patricia Salas", "it.member.owner@example.com"), "accountId")
        val ownerToken = field(signIn("it.member.owner@example.com"), "token")
        signUp("Diego Salas", "it.member.viewer@example.com")
        mockMvc.perform(post("/api/v1/subscriptions/trial").header("Authorization", "Bearer $ownerToken"))
            .andExpect(status().isCreated)

        val invitation = mockMvc.perform(
            post("/api/v1/invitations").header("Authorization", "Bearer $ownerToken").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"it.member.viewer@example.com","permission":"VIEWER"}""")
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val memberOwnToken = field(signIn("it.member.viewer@example.com"), "token")
        mockMvc.perform(
            post("/api/v1/invitations/${field(invitation, "id")}/acceptance").header("Authorization", "Bearer $memberOwnToken")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/authentication/sign-in").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"it.member.viewer@example.com","password":"Qlic2026!"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.permission").value("OWNER"))
            .andExpect(jsonPath("$.sharedAccounts.length()").value(1))
            .andExpect(jsonPath("$.sharedAccounts[0].accountId").value(ownerAccountId))
            .andExpect(jsonPath("$.sharedAccounts[0].permission").value("VIEWER"))

        val session = mockMvc.perform(
            post("/api/v1/accounts/$ownerAccountId/sessions").header("Authorization", "Bearer $memberOwnToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(ownerAccountId))
            .andExpect(jsonPath("$.permission").value("VIEWER"))
            .andReturn().response.contentAsString
        val memberToken = field(session, "token")

        mockMvc.perform(get("/api/v1/accounts/me").header("Authorization", "Bearer $memberToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accountId").value(ownerAccountId))
            .andExpect(jsonPath("$.permission").value("VIEWER"))
        mockMvc.perform(get("/api/v1/alerts").header("Authorization", "Bearer $memberToken"))
            .andExpect(status().isOk)
        mockMvc.perform(
            post("/api/v1/locations").header("Authorization", "Bearer $memberToken").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Otra sede"}""")
        ).andExpect(status().isForbidden)

        val outsiderToken = field(signIn(field(signUp("Ana Ruiz", "it.member.outsider@example.com"), "email")), "token")
        mockMvc.perform(
            post("/api/v1/accounts/$ownerAccountId/sessions").header("Authorization", "Bearer $outsiderToken")
        ).andExpect(status().isForbidden)
    }
}
