package com.wasd.qlic.iam.domain.model.aggregates

import com.wasd.qlic.iam.domain.model.entities.Subscriber
import java.time.Instant
import java.util.UUID

/**
 * Aggregate Root: subscriber account. It groups the account holder and, in later
 * stories (US08), the authorized users.
 */
class Account(
    val id: UUID = UUID.randomUUID(),
    val ownerId: UUID,
    var name: String,
    val createdAt: Instant = Instant.now()
) {
    companion object {
        /** Opens the account of a new account holder (US05). */
        fun openFor(owner: Subscriber): Account =
            Account(ownerId = owner.id, name = "${owner.fullName}'s account")
    }
}
