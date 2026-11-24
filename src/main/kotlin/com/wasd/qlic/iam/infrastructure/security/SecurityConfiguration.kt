package com.wasd.qlic.iam.infrastructure.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Configures the security filter chain and the protected routes of the API.
 * Authentication endpoints, the terms of service and the OpenAPI documentation are public; every
 * other request requires a valid Bearer token (TS01 - Scenario 3: 401 without token).
 * Modification requests over the account resources require the account:write scope, which is not
 * granted to VIEWER users (US08 - Scenario 2: 403).
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    @Value("\${qlic.security.jwt.secret}") private val jwtSecret: String,
    private val accessDeniedHandler: ProblemDetailAccessDeniedHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers { headers -> headers.frameOptions { it.sameOrigin() } }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
                WRITE_METHODS.forEach { method ->
                    auth.requestMatchers(method, *WRITE_PROTECTED_ENDPOINTS).hasAuthority(WRITE_AUTHORITY)
                }
                auth.anyRequest().authenticated()
            }
            .exceptionHandling { it.accessDeniedHandler(accessDeniedHandler) }
            .oauth2ResourceServer {
                it.jwt(Customizer.withDefaults())
                it.accessDeniedHandler(accessDeniedHandler)
            }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey()))

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(secretKey())
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("qlic-backend-api"))
        return decoder
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept-Language")
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    private fun secretKey(): SecretKey {
        val bytes = jwtSecret.toByteArray(Charsets.UTF_8)
        require(bytes.size >= 32) { "qlic.security.jwt.secret must have at least 32 bytes for HS256" }
        return SecretKeySpec(bytes, "HmacSHA256")
    }

    companion object {
        private val PUBLIC_ENDPOINTS = arrayOf(
            "/api/v1/authentication/**",
            "/api/v1/terms-of-service/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/h2-console/**",
            "/error"
        )

        /** Resources of the account whose configuration a VIEWER cannot modify (US08 - Scenario 2). */
        private val WRITE_PROTECTED_ENDPOINTS = arrayOf(
            "/api/v1/devices/**",
            "/api/v1/alerts/**",
            "/api/v1/consumption/**",
            "/api/v1/consumption-readings/**",
            "/api/v1/incidents/**",
            "/api/v1/subscriptions/**"
        )

        private val WRITE_METHODS = listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)

        private const val WRITE_AUTHORITY = "SCOPE_account:write"
    }
}
