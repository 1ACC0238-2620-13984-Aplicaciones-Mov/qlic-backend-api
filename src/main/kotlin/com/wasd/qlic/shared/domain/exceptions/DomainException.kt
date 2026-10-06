package com.wasd.qlic.shared.domain.exceptions

/**
 * Base exception for business rule violations. It carries a message key that the interface
 * layer resolves with the requested locale (en_US by default, es_419 optional).
 */
abstract class DomainException(
    val messageKey: String,
    vararg val arguments: Any
) : RuntimeException(messageKey)
