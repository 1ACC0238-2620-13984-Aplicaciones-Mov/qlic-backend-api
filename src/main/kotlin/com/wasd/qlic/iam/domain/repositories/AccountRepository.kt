package com.wasd.qlic.iam.domain.repositories

import com.wasd.qlic.iam.domain.model.aggregates.Account
import java.util.UUID

interface AccountRepository {
    fun save(account: Account): Account
    fun findByOwnerId(ownerId: UUID): Account?
}
