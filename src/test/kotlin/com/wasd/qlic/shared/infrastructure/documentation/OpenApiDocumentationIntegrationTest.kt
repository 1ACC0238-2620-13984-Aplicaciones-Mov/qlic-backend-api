package com.wasd.qlic.shared.infrastructure.documentation

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** The published OpenAPI document describes every operation and gives examples of requests and errors. */
@SpringBootTest
class OpenApiDocumentationIntegrationTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private fun apiDocs(): String =
        MockMvcBuilders.webAppContextSetup(context).apply<DefaultMockMvcBuilder>(springSecurity()).build()
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

    @Test
    @DisplayName("Every operation has a summary and a description")
    fun everyOperationIsDescribed() {
        val json = apiDocs()
        val operations = JsonPath.read<List<Any>>(json, "$.paths.*.*.operationId")
        val summaries = JsonPath.read<List<Any>>(json, "$.paths.*.*.summary")
        val descriptions = JsonPath.read<List<Any>>(json, "$.paths.*.*.description")

        assertTrue(operations.size >= 80, "operations: ${operations.size}")
        assertEquals(operations.size, summaries.size)
        assertEquals(operations.size, descriptions.size)
    }

    @Test
    @DisplayName("Requests and errors carry examples")
    fun requestsAndErrorsHaveExamples() {
        val json = apiDocs()

        assertEquals(
            "QS-2026-000123",
            JsonPath.read<String>(json, "$.components.schemas.RegisterDeviceRequest.properties.serialNumber.example")
        )
        assertEquals(
            "monitoring.device.not-found",
            JsonPath.read<String>(json, "$.paths['/api/v1/devices/{id}'].get.responses['404'].content['application/problem+json'].example.code")
        )
        assertTrue(JsonPath.read<Map<String, Any>>(json, "$.paths['/api/v1/devices'].post.responses").containsKey("401"))
        assertTrue(JsonPath.read<Map<String, Any>>(json, "$.paths['/api/v1/devices/{id}'].put.responses").containsKey("403"))
        assertTrue(!JsonPath.read<Map<String, Any>>(json, "$.paths['/api/v1/plans'].get.responses").containsKey("401"))
    }
}
