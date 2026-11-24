package com.wasd.qlic.support.domain.model

import java.util.UUID

/** Entity: support staff assignable to a technical visit (US36). */
class Technician(
    val id: UUID = UUID.randomUUID(),
    val fullName: String,
    val phone: String,
    val zone: String,
    val active: Boolean = true
)
