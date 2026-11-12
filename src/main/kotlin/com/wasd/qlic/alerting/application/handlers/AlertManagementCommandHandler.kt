package com.wasd.qlic.alerting.application.handlers

import com.wasd.qlic.alerting.application.commands.*
import com.wasd.qlic.alerting.domain.model.*
import com.wasd.qlic.alerting.domain.repositories.*
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AlertManagementCommandHandler(
    private val leakAlertRepository: LeakAlertRepository,
    private val thresholdRepository: ConsumptionThresholdRepository,
    private val preferenceRepository: NotificationPreferenceRepository,
    private val recipientRepository: AuthorizedAlertRecipientRepository
) {
    // US16 (Detail): Obtener detalle de una alerta específica por ID y cuenta
    fun getAlertDetail(alertId: UUID, accountId: UUID): LeakAlert? {
        val alert = leakAlertRepository.findById(alertId) ?: return null
        return if (alert.accountId == accountId) alert else null
    }

    // US17: Configurar umbral de consumo (propio o automático sobre línea base)
    fun configureThreshold(command: ConfigureThresholdCommand): ConsumptionThreshold {
        if (command.customLimitLitersPerHour != null) {
            require(command.customLimitLitersPerHour > java.math.BigDecimal.ZERO) {
                "El valor del umbral debe ser mayor a cero"
            }
        }
        val existing = thresholdRepository.findByAccountAndWaterPoint(command.accountId, command.waterPointId)
        val source = if (command.customLimitLitersPerHour != null) {
            ThresholdSource.USER_DEFINED
        } else {
            ThresholdSource.SYSTEM_CALCULATED
        }

        val threshold = ConsumptionThreshold(
            id = existing?.id ?: UUID.randomUUID(),
            accountId = command.accountId,
            waterPointId = command.waterPointId,
            period = command.period,
            customLimitLitersPerHour = command.customLimitLitersPerHour,
            baselineLitersPerHour = command.baselineLitersPerHour,
            source = source,
            updatedAt = LocalDateTime.now()
        )
        return thresholdRepository.save(threshold)
    }

    fun getThresholdsByAccount(accountId: UUID): List<ConsumptionThreshold> =
        thresholdRepository.findAllByAccountId(accountId)

    // US18: Limitar la frecuencia de notificaciones y nivel mínimo de urgencia
    fun configureNotificationPreference(command: ConfigureNotificationPreferenceCommand): NotificationPreference {
        require(command.groupingWindowMinutes > 0) { "La ventana de agrupación debe ser mayor a 0 minutos" }
        val existing = preferenceRepository.findByAccountId(command.accountId)
        val preference = NotificationPreference(
            id = existing?.id ?: UUID.randomUUID(),
            accountId = command.accountId,
            groupingWindowMinutes = command.groupingWindowMinutes,
            minimumUrgency = command.minimumUrgency,
            updatedAt = LocalDateTime.now()
        )
        return preferenceRepository.save(preference)
    }

    fun getNotificationPreference(accountId: UUID): NotificationPreference =
        preferenceRepository.findByAccountId(accountId)
            ?: NotificationPreference(accountId = accountId)

    // US19: Gestionar y notificar a usuarios autorizados de la cuenta
    fun addAuthorizedRecipient(command: AddAuthorizedRecipientCommand): AuthorizedAlertRecipient {
        val recipient = AuthorizedAlertRecipient(
            accountId = command.accountId,
            fullName = command.fullName,
            email = command.email,
            devicePushToken = command.devicePushToken,
            enabled = command.enabled
        )
        return recipientRepository.save(recipient)
    }

    fun getRecipientsByAccount(accountId: UUID): List<AuthorizedAlertRecipient> =
        recipientRepository.findAllByAccountId(accountId)

    fun toggleRecipient(recipientId: UUID, enabled: Boolean): AuthorizedAlertRecipient {
        val recipient = recipientRepository.findById(recipientId)
            ?: throw IllegalArgumentException("Usuario autorizado no encontrado: $recipientId")
        recipient.enabled = enabled
        return recipientRepository.save(recipient)
    }

    fun dispatchPushToAuthorizedUsers(accountId: UUID): List<AuthorizedAlertRecipient> {
        val enabledUsers = recipientRepository.findEnabledByAccountId(accountId)
        enabledUsers.forEach { it.markNotified() }
        return enabledUsers.map { recipientRepository.save(it) }
    }

    // US20: Confirmar la atención de una alerta activa registrando la acción realizada
    fun acknowledgeAlert(command: AcknowledgeAlertCommand): LeakAlert {
        val alert = leakAlertRepository.findById(command.alertId)
            ?.takeIf { it.accountId == command.accountId }
            ?: throw IllegalArgumentException("Alerta no encontrada para la cuenta indicada")

        alert.acknowledge(command.actionTaken)
        return leakAlertRepository.save(alert)
    }
}