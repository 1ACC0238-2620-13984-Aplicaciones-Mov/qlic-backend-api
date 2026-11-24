package com.wasd.qlic.iam.infrastructure.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.LocaleResolver

/**
 * US08 - Scenario 2: when a token with VIEWER permission tries to modify a resource, the API
 * answers 403 with a localized Problem Details body (RFC 9457), as the rest of the errors.
 */
@Component
class ProblemDetailAccessDeniedHandler(
    private val messageSource: MessageSource,
    private val localeResolver: LocaleResolver
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException
    ) {
        val status = HttpStatus.FORBIDDEN
        val detail = messageSource.getMessage(MESSAGE_KEY, null, MESSAGE_KEY, localeResolver.resolveLocale(request))
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            """{"type":"about:blank","title":"${status.reasonPhrase}","status":${status.value()},""" +
                """"detail":"${escape(detail ?: MESSAGE_KEY)}","instance":"${escape(request.requestURI)}",""" +
                """"code":"$MESSAGE_KEY"}"""
        )
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        const val MESSAGE_KEY = "iam.permission.denied"
    }
}
