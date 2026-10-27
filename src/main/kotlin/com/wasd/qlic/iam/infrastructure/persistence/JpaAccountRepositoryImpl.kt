package com.wasd.qlic.iam.infrastructure.persistence

import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.repositories.AccountRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaAccountRepositoryImpl(
    private val jpaRepository: SpringDataAccountRepository
) : AccountRepository {

    override fun save(account: Account): Account =
        toDomain(
            jpaRepository.save(
                AccountEntity(
                    id = account.id,
                    ownerId = account.ownerId,
                    name = account.name,
                    createdAt = account.createdAt
                )
            )
        )

    override fun findByOwnerId(ownerId: UUID): Account? =
        jpaRepository.findFirstByOwnerId(ownerId)?.let { toDomain(it) }

    private fun toDomain(entity: AccountEntity) = Account(
        id = entity.id,
        ownerId = entity.ownerId,
        name = entity.name,
        createdAt = entity.createdAt
    )
}
