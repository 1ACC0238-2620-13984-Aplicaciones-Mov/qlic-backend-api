package com.wasd.qlic.iam.application.queries

import java.util.UUID

/** US09: profile of the requester and data of the account of the session. */
data class GetProfileQuery(val subscriberId: UUID, val accountId: UUID)

/** US08: invitations issued by an account. */
data class GetInvitationsByAccountQuery(val accountId: UUID)

/** US08: pending invitations addressed to the requester. */
data class GetReceivedInvitationsQuery(val subscriberId: UUID)

/** US08: authorized users of an account. */
data class GetAuthorizedUsersQuery(val accountId: UUID)

/** US08: accounts shared with the requester. */
data class GetSharedAccountsQuery(val subscriberId: UUID)
