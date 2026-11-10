package com.wasd.qlic.support.interfaces.rest

import com.wasd.qlic.support.application.handlers.SupportQueryHandler
import com.wasd.qlic.support.application.queries.GetTermsOfServiceQuery
import com.wasd.qlic.support.interfaces.rest.resources.TermsOfServiceResource
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/terms-of-service")
@Tag(name = "Terms of Service", description = "Support - Terms of service and personal data policy (US38)")
class TermsOfServiceController(
    private val supportQueryHandler: SupportQueryHandler
) {

    @GetMapping("/current")
    @SecurityRequirements // public: shown in the Landing Page and in the sign-up screen
    @Operation(
        summary = "Get the current terms of service (US38)",
        description = "Returns the current document with its version and update date. Send Accept-Language: es-419 for Spanish."
    )
    fun getCurrent(): ResponseEntity<TermsOfServiceResource> {
        val terms = supportQueryHandler.handle(GetTermsOfServiceQuery(LocaleContextHolder.getLocale().language))
        return ResponseEntity.ok(TermsOfServiceResource(terms.version, terms.language, terms.title, terms.content, terms.updatedOn))
    }
}
