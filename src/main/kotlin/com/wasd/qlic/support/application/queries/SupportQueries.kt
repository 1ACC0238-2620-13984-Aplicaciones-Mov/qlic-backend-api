package com.wasd.qlic.support.application.queries

import java.util.UUID

/** US37: incidents of an account. */
data class GetIncidentsByAccountQuery(val accountId: UUID)

/** US37: detail of an incident of the account. */
data class GetIncidentByIdQuery(val incidentId: UUID, val accountId: UUID)

/** US38: current terms of service in the requested language. */
data class GetTermsOfServiceQuery(val language: String)
