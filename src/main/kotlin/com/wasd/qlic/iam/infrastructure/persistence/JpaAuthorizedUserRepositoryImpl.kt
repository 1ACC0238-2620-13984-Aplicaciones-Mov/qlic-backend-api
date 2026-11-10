package com.wasd.qlic.iam.infrastructure.persistence

import com.wasd.qlic.iam.domain.model.entities.AuthorizedUser
import com.wasd.qlic.iam.domain.repositories.AuthorizedUserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaAuthorizedUserRepositoryImpl(
    private val jpaRepository: SpringDataAuthorizedUserRepository
) : AuthorizedUserRepository {

    override fun save(authorizedUser: AuthorizedUser): AuthorizedUser =
        toDomain(
            jpaRepository.save(
                AuthorizedUserEntity(
                    id = authorizedUser.id,
                    accountId = authorizedUser.accountId,
                    subscriberId = authorizedUser.subscriberId,
                    permission = authorizedUser.permission,
                    invitationId = authorizedUser.invitationId,
                    authorizedAt = authorizedUser.authorizedAt
                )
            )
        )

    override fun findById(id: UUID): AuthorizedUser? =
        jpaRepository.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<AuthorizedUser> =
        jpaRepository.findAllByAccountId(accountId).map { toDomain(it) }

    override fun findBySubscriberId(subscriberId: UUID): List<AuthorizedUser> =
        jpaRepository.findAllBySubscriberId(subscriberId).map { toDomain(it) }

    override fun findByAccountIdAndSubscriberId(accountId: UUID, subscriberId: UUID): AuthorizedUser? =
        jpaRepository.findFirstByAccountIdAndSubscriberId(accountId, subscriberId)?.let { toDomain(it) }

    override fun delete(authorizedUser: AuthorizedUser) =
        jpaRepository.deleteById(authorizedUser.id)

    private fun toDomain(entity: AuthorizedUserEntity) = AuthorizedUser(
        id = entity.id,
        accountId = entity.accountId,
        subscriberId = entity.subscriberId,
        permission = entity.permission,
        invitationId = entity.invitationId,
        authorizedAt = entity.authorizedAt
    )
}
