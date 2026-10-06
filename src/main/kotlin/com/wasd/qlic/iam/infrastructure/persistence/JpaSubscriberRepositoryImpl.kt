package com.wasd.qlic.iam.infrastructure.persistence

import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.model.valueobjects.PasswordHash
import com.wasd.qlic.iam.domain.repositories.SubscriberRepository
import org.springframework.stereotype.Component

@Component
class JpaSubscriberRepositoryImpl(
    private val jpaRepository: SpringDataSubscriberRepository
) : SubscriberRepository {

    override fun save(subscriber: Subscriber): Subscriber =
        toDomain(
            jpaRepository.save(
                SubscriberEntity(
                    id = subscriber.id,
                    email = subscriber.email.value,
                    passwordHash = subscriber.passwordHash.value,
                    fullName = subscriber.fullName,
                    phone = subscriber.phone,
                    termsAcceptedAt = subscriber.termsAcceptedAt,
                    createdAt = subscriber.createdAt
                )
            )
        )

    override fun findByEmail(email: EmailAddress): Subscriber? =
        jpaRepository.findByEmail(email.value)?.let { toDomain(it) }

    override fun existsByEmail(email: EmailAddress): Boolean =
        jpaRepository.existsByEmail(email.value)

    private fun toDomain(entity: SubscriberEntity) = Subscriber(
        id = entity.id,
        email = EmailAddress(entity.email),
        passwordHash = PasswordHash(entity.passwordHash),
        fullName = entity.fullName,
        phone = entity.phone,
        termsAcceptedAt = entity.termsAcceptedAt,
        createdAt = entity.createdAt
    )
}
