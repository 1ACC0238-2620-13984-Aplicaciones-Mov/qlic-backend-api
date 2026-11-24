package com.wasd.qlic.iam.domain.repositories

import com.wasd.qlic.iam.domain.model.entities.Invitation
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress
import java.util.UUID

interface InvitationRepository {
    fun save(invitation: Invitation): Invitation
    fun findById(id: UUID): Invitation?
    fun findByAccountId(accountId: UUID): List<Invitation>
    fun findByInvitedEmail(email: EmailAddress): List<Invitation>
}
