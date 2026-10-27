package com.wasd.qlic.iam.domain.model.valueobjects

import com.wasd.qlic.iam.domain.exceptions.InvalidEmailException

/**
 * Value Object: a valid email address, normalized to lower case so that it is unique in the system.
 */
data class EmailAddress(val value: String) {
    init {
        if (!PATTERN.matches(value)) throw InvalidEmailException()
    }

    companion object {
        private val PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun of(raw: String): EmailAddress = EmailAddress(raw.trim().lowercase())
    }
}
