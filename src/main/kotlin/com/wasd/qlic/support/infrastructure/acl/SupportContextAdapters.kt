package com.wasd.qlic.support.infrastructure.acl

import com.wasd.qlic.alerting.domain.model.AlertState
import com.wasd.qlic.alerting.domain.repositories.LeakAlertRepository
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import com.wasd.qlic.subscriptions.domain.repositories.SubscriptionRepository
import com.wasd.qlic.support.domain.services.AlertData
import com.wasd.qlic.support.domain.services.AlertDataProvider
import com.wasd.qlic.support.domain.services.DeviceData
import com.wasd.qlic.support.domain.services.DeviceDataProvider
import com.wasd.qlic.support.domain.services.PlanEntitlementProvider
import org.springframework.stereotype.Component
import java.util.UUID

/** Anti-corruption layer with Alerting: translates a LeakAlert into the data Support needs (US35). */
@Component
class AlertDataAdapter(
    private val leakAlertRepository: LeakAlertRepository
) : AlertDataProvider {
    override fun findAlert(alertId: UUID, accountId: UUID): AlertData? =
        leakAlertRepository.findById(alertId)
            ?.takeIf { it.accountId == accountId }
            ?.let { AlertData(alertId = it.id, waterPointId = it.waterPointId, active = it.state == AlertState.ACTIVE) }
}

/** Anti-corruption layer with Device Monitoring: verifies the device belongs to the account (US35). */
@Component
class DeviceDataAdapter(
    private val deviceRepository: IoTDeviceRepository
) : DeviceDataProvider {
    override fun findDevice(deviceId: UUID, accountId: UUID): DeviceData? =
        deviceRepository.findById(deviceId)
            ?.takeIf { it.accountId == accountId }
            ?.let { DeviceData(deviceId = it.id, waterPointId = it.waterPointId) }
}

/**
 * Anti-corruption layer with Subscriptions (US36): any plan or trial with access to the paid features
 * covers technical visits.
 */
@Component
class SupportPlanEntitlementAdapter(
    private val subscriptionRepository: SubscriptionRepository
) : PlanEntitlementProvider {
    override fun coversTechnicalVisits(accountId: UUID): Boolean =
        subscriptionRepository.findByAccountId(accountId)?.hasPaidFeaturesAccess() ?: false
}
