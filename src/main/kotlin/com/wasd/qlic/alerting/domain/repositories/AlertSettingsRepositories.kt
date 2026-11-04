package com.wasd.qlic.alerting.domain.repositories

import com.wasd.qlic.alerting.domain.model.AuthorizedAlertRecipient
import com.wasd.qlic.alerting.domain.model.ConsumptionThreshold
import com.wasd.qlic.alerting.domain.model.NotificationPreference
import java.util.UUID

interface ConsumptionThresholdRepository {
    fun save(threshold: ConsumptionThreshold): ConsumptionThreshold
    fun findByAccountAndWaterPoint(accountId: UUID, waterPointId: UUID): ConsumptionThreshold?
    fun findAllByAccountId(accountId: UUID): List<ConsumptionThreshold>
}

interface NotificationPreferenceRepository {
    fun save(preference: NotificationPreference): NotificationPreference
    fun findByAccountId(accountId: UUID): NotificationPreference?
}

interface AuthorizedAlertRecipientRepository {
    fun save(recipient: AuthorizedAlertRecipient): AuthorizedAlertRecipient
    fun findById(id: UUID): AuthorizedAlertRecipient?
    fun findAllByAccountId(accountId: UUID): List<AuthorizedAlertRecipient>
    fun findEnabledByAccountId(accountId: UUID): List<AuthorizedAlertRecipient>
}