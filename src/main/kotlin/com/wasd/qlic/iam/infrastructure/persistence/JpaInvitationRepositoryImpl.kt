package com.wasd.qlic.iam.infrastructure.persistence

import com.wasd.qlic.iam.domain.model.entities.Invitation
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import com.wasd.qlic.iam.domain.repositories.InvitationRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaInvitationRepositoryImpl(
    private val jpaRepository: SpringDataInvitationRepository
) : InvitationRepository {

    override fun save(invitation: Invitation): Invitation =
        toDomain(
            jpaRepository.save(
                InvitationEntity(
                    id = invitation.id,
                    accountId = invitation.accountId,
                    invitedEmail = invitation.invitedEmail.value,
                    permission = invitation.permission,
                    invitedBy = invitation.invitedBy,
                    state = invitation.state,
                    createdAt = invitation.createdAt,
                    expiresAt = invitation.expiresAt,
                    acceptedAt = invitation.acceptedAt
                )
            )
        )

    override fun findById(id: UUID): Invitation? =
        jpaRepository.findById(id).orElse(null)?.let { toDomain(it) }

    override fun findByAccountId(accountId: UUID): List<Invitation> =
        jpaRepository.findAllByAccountId(accountId).map { toDomain(it) }

    override fun findByInvitedEmail(email: EmailAddress): List<Invitation> =
        jpaRepository.findAllByInvitedEmail(email.value).map { toDomain(it) }

    private fun toDomain(entity: InvitationEntity) = Invitation(
        id = entity.id,
        accountId = entity.accountId,
        invitedEmail = EmailAddress(entity.invitedEmail),
        permission = entity.permission,
        invitedBy = entity.invitedBy,
        state = entity.state,
        createdAt = entity.createdAt,
        expiresAt = entity.expiresAt,
        acceptedAt = entity.acceptedAt
    )
}
