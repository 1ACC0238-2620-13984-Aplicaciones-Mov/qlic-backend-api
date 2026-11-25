package com.wasd.qlic.subscriptions.interfaces.rest

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** TS07 - Scenario 1: the catalog of plans is public and ordered by price. */
@SpringBootTest
class PlanIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
    }

    @Test
    @DisplayName("TS07 - Scenario 1: the plans are listed without a token, ordered by price")
    fun plansArePublic() {
        mockMvc.perform(get("/api/v1/plans"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].plan").value("TRIAL"))
            .andExpect(jsonPath("$[0].purchasable").value(false))
            .andExpect(jsonPath("$[0].billingPeriodDays").value(14))
            .andExpect(jsonPath("$[4].plan").value("PYME_ENTERPRISE"))
            .andExpect(jsonPath("$[4].currency").value("PEN"))
    }

    @Test
    @DisplayName("TS07: the catalog can be filtered by segment and without the trial")
    fun plansCanBeFiltered() {
        mockMvc.perform(get("/api/v1/plans").param("segment", "HOME"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].plan").value("HOME_BASIC"))

        mockMvc.perform(get("/api/v1/plans").param("includeTrial", "false"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
    }

    @Test
    @DisplayName("TS07: one plan shows its limits and an unknown plan is a bad request")
    fun planDetail() {
        mockMvc.perform(get("/api/v1/plans/HOME_PRO"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.monthlyPrice").value(49.90))
            .andExpect(jsonPath("$.maxWaterPoints").value(6))
            .andExpect(jsonPath("$.maxAuthorizedUsers").value(3))

        mockMvc.perform(get("/api/v1/plans/GOLD"))
            .andExpect(status().isBadRequest)
    }
}
