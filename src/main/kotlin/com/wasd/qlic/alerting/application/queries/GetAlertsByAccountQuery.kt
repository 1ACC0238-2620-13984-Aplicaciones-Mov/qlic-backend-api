package com.wasd.qlic.alerting.application.queries

import java.util.UUID

data class GetAlertsByAccountQuery(
    val accountId: UUID
)