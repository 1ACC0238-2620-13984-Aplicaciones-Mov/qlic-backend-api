package com.wasd.qlic.monitoring.application.queries

import java.util.UUID

data class GetDevicesByAccountQuery(
    val accountId: UUID
)