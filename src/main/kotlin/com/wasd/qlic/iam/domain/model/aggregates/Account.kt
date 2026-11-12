package com.wasd.qlic.iam.domain.model.aggregates

import com.wasd.qlic.iam.domain.model.entities.Subscriber
import java.time.Instant
import java.util.UUID

/**
 * Aggregate Root: subscriber account. It groups the account holder and the
 * authorized users (US08). Its name identifies the household or business premises (US09).
 */
class Account(
    val id: UUID = UUID.randomUUID(),
    val ownerId: UUID,
    var name: String,
    val createdAt: Instant = Instant.now()
) {
    /** US09: updates the name of the household or business premises. */
    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Account name is required" }
        name = newName.trim()
    }

    companion object {
        /** Opens the account of a new account holder (US05). */
        fun openFor(owner: Subscriber): Account =
            Account(ownerId = owner.id, name = "${owner.fullName}'s account")
    }
}
