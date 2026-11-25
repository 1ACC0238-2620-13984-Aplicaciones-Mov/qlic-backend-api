package com.wasd.qlic.monitoring.application.queries

import java.time.LocalDate
import java.util.UUID

/** US12 and US13: devices of the account, optionally only those of one location. */
data class GetDevicesByAccountQuery(val accountId: UUID, val locationId: UUID? = null)

data class GetDeviceByIdQuery(val deviceId: UUID, val accountId: UUID)

data class GetLocationsQuery(val accountId: UUID)

data class GetLocationByIdQuery(val locationId: UUID, val accountId: UUID)

/** US13: Water Points of the account, optionally only those of one location. */
data class GetWaterPointsQuery(val accountId: UUID, val locationId: UUID? = null)

data class GetWaterPointByIdQuery(val waterPointId: UUID, val accountId: UUID)

/** US13 - Scenario 1: readings of a period, optionally only those of one location or Water Point. */
data class GetReadingsByPeriodQuery(
    val accountId: UUID,
    val from: LocalDate,
    val to: LocalDate,
    val locationId: UUID? = null,
    val waterPointId: UUID? = null
)

/** Consolidated monitoring dashboard of the account, optionally of one location. */
data class GetMonitoringDashboardQuery(
    val accountId: UUID,
    val locationId: UUID? = null,
    val now: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
