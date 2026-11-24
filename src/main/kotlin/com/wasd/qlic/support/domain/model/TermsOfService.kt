package com.wasd.qlic.support.domain.model

import java.time.LocalDate
import java.util.UUID

/**
 * Entity: version of the terms of service and the personal data treatment policy, in one language (US38).
 */
class TermsOfService(
    val id: UUID = UUID.randomUUID(),
    val version: String,
    val language: String,
    val title: String,
    val content: String,
    val updatedOn: LocalDate
)
