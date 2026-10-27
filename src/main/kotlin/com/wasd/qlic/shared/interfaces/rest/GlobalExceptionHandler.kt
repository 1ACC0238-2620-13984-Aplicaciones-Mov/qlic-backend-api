package com.wasd.qlic.shared.interfaces.rest

import com.wasd.qlic.iam.domain.exceptions.EmailAlreadyRegisteredException
import com.wasd.qlic.iam.domain.exceptions.InvalidCredentialsException
import com.wasd.qlic.shared.domain.exceptions.DomainException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Translates exceptions into "Problem Details for HTTP APIs" (RFC 9457) responses,
 * with localized messages (en_US by default, es_419 via Accept-Language).
 */
@RestControllerAdvice
class GlobalExceptionHandler(private val messageSource: MessageSource) {

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(exception: DomainException): ResponseEntity<ProblemDetail> =
        problem(statusOf(exception), exception.messageKey, *exception.arguments)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val errors = linkedMapOf<String, String>()
        exception.bindingResult.fieldErrors.forEach { errors.putIfAbsent(it.field, it.defaultMessage ?: "") }
        val response = problem(HttpStatus.BAD_REQUEST, "error.validation")
        response.body?.setProperty("errors", errors)
        return response
    }

    @ExceptionHandler(HttpMessageNotReadableException::class, IllegalArgumentException::class)
    fun handleBadRequest(exception: Exception): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "error.invalid-request")

    private fun statusOf(exception: DomainException): HttpStatus = when (exception) {
        is EmailAlreadyRegisteredException -> HttpStatus.CONFLICT
        is InvalidCredentialsException -> HttpStatus.UNAUTHORIZED
        else -> HttpStatus.BAD_REQUEST
    }

    private fun problem(status: HttpStatus, messageKey: String, vararg arguments: Any): ResponseEntity<ProblemDetail> {
        val detail = messageSource.getMessage(messageKey, arguments, messageKey, LocaleContextHolder.getLocale())
        val body = ProblemDetail.forStatusAndDetail(status, detail ?: messageKey)
        body.title = status.reasonPhrase
        body.setProperty("code", messageKey)
        return ResponseEntity.status(status).body(body)
    }
}
