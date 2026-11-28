package com.wasd.qlic.iam.infrastructure.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.LocaleResolver

/**
 * TS01 - Scenario 3: a request to a protected resource without a token, or with an expired or invalid one, gets
 * 401 with a localized Problem Details body, like the rest of the errors. The code tells the applications to send
 * the user back to the sign-in screen.
 */
@Component
class ProblemDetailAuthenticationEntryPoint(
    private val messageSource: MessageSource,
    private val localeResolver: LocaleResolver
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val status = HttpStatus.UNAUTHORIZED
        val key = if (request.getHeader(HttpHeaders.AUTHORIZATION).isNullOrBlank()) MISSING_TOKEN_KEY else INVALID_TOKEN_KEY
        val detail = messageSource.getMessage(key, null, key, localeResolver.resolveLocale(request))
        response.status = status.value()
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            """{"type":"about:blank","title":"${status.reasonPhrase}","status":${status.value()},""" +
                """"detail":"${escape(detail ?: key)}","instance":"${escape(request.requestURI)}",""" +
                """"code":"$key"}"""
        )
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        const val MISSING_TOKEN_KEY = "iam.token.required"
        const val INVALID_TOKEN_KEY = "iam.token.invalid"
    }
}
