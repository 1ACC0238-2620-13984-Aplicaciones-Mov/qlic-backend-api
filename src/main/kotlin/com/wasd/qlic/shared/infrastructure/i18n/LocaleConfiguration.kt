package com.wasd.qlic.shared.infrastructure.i18n

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver
import java.util.Locale

/**
 * Internationalization (i18n): resolves the locale from the Accept-Language header.
 * Supported locales are English (en_US, default) and Latin American Spanish (es_419).
 */
@Configuration
class LocaleConfiguration {

    @Bean
    fun localeResolver(): LocaleResolver = AcceptHeaderLocaleResolver().apply {
        supportedLocales = listOf(DEFAULT_LOCALE, LATIN_AMERICAN_SPANISH)
        setDefaultLocale(DEFAULT_LOCALE)
    }

    companion object {
        val DEFAULT_LOCALE: Locale = Locale.US
        val LATIN_AMERICAN_SPANISH: Locale = Locale.forLanguageTag("es-419")
    }
}
