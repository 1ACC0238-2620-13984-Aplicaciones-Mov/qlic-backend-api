package com.wasd.qlic.support.domain.services

import java.time.LocalDateTime
import java.util.UUID

/** Data of the alert that originates an incident (US35 - Scenario 2). */
data class AlertData(val alertId: UUID, val waterPointId: UUID, val active: Boolean)

/** Domain port: alert data, provided by the Alerting bounded context. */
interface AlertDataProvider {
    fun findAlert(alertId: UUID, accountId: UUID): AlertData?
}

/** Data of the affected device (US35 - Scenario 1). */
data class DeviceData(val deviceId: UUID, val waterPointId: UUID?)

/** Domain port: device data, provided by the Device Monitoring bounded context. */
interface DeviceDataProvider {
    fun findDevice(deviceId: UUID, accountId: UUID): DeviceData?
}

/** Domain port: technical visit coverage of the current plan, provided by Subscriptions (US36). */
interface PlanEntitlementProvider {
    fun coversTechnicalVisits(accountId: UUID): Boolean
}

/** Domain port: notifications to the subscriber about the attention of their incidents (US36 - Scenario 2). */
interface SupportNotificationSender {
    fun notifyTechnicianAssigned(accountId: UUID, incidentId: UUID, technicianName: String, scheduledAt: LocalDateTime)
}
