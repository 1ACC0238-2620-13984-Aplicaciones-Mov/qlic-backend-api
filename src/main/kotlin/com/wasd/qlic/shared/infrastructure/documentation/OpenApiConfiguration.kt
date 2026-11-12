package com.wasd.qlic.shared.infrastructure.documentation

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI 3 documentation of the Qlic RESTful API, published with Swagger UI
 * at /swagger-ui.html and as JSON at /v3/api-docs.
 */
@Configuration
@SecurityScheme(
    name = OpenApiConfiguration.BEARER_SCHEME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Access token returned by POST /api/v1/authentication/sign-in"
)
class OpenApiConfiguration {

    @Bean
    fun qlicOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Qlic RESTful API")
                .description(
                    "RESTful services of Qlic, a smart water management solution with IoT for small " +
                        "businesses and households. Startup WASD - UPC 1ACC0238 Mobile Applications. " +
                        "Send Accept-Language: es-419 to receive messages in Latin American Spanish; " +
                        "English is the default."
                )
                .version("v1.0.0")
                .contact(Contact().name("WASD").url("https://github.com/1ACC0238-2620-13984-Aplicaciones-Mov"))
                .license(License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0"))
        )
        .externalDocs(
            ExternalDocumentation()
                .description("Qlic Project Report")
                .url("https://github.com/1ACC0238-2620-13984-Aplicaciones-Mov/upc-pre-202620-1acc0238-13984-Qlic-report")
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER_SCHEME))

    companion object {
        const val BEARER_SCHEME = "bearerAuth"
    }
}
