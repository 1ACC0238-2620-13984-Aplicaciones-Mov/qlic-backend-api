package com.wasd.qlic.iam.domain.repositories

import com.wasd.qlic.iam.domain.model.entities.AuthorizedUser
import java.util.UUID

interface AuthorizedUserRepository {
    fun save(authorizedUser: AuthorizedUser): AuthorizedUser
    fun findById(id: UUID): AuthorizedUser?
    fun findByAccountId(accountId: UUID): List<AuthorizedUser>
    fun findBySubscriberId(subscriberId: UUID): List<AuthorizedUser>
    fun findByAccountIdAndSubscriberId(accountId: UUID, subscriberId: UUID): AuthorizedUser?
    fun delete(authorizedUser: AuthorizedUser)
}
