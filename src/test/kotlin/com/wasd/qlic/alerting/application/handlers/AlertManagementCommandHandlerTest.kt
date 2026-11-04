package com.wasd.qlic.alerting.application.handlers

import com.wasd.qlic.alerting.application.commands.*
import com.wasd.qlic.alerting.domain.model.*
import com.wasd.qlic.alerting.domain.repositories.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class AlertManagementCommandHandlerTest {

    private lateinit var alertRepo: LeakAlertRepository
    private lateinit var thresholdRepo: ConsumptionThresholdRepository
    private lateinit var preferenceRepo: NotificationPreferenceRepository
    private lateinit var recipientRepo: AuthorizedAlertRecipientRepository
    private lateinit var handler: AlertManagementCommandHandler

    @BeforeEach
    fun setUp() {
        alertRepo = object : LeakAlertRepository {
            private val store = mutableMapOf<UUID, LeakAlert>()

            override fun save(alert: LeakAlert): LeakAlert {
                store[alert.id] = alert
                return alert
            }

            override fun findById(id: UUID): LeakAlert? = store[id]

            override fun findByAccountId(accountId: UUID): List<LeakAlert> =
                store.values.filter { it.accountId == accountId }

            override fun findActiveByWaterPointId(waterPointId: UUID): LeakAlert? =
                store.values.find { it.waterPointId == waterPointId && it.isActive() }
        }

        thresholdRepo = object : ConsumptionThresholdRepository {
            private val store = mutableMapOf<UUID, ConsumptionThreshold>()

            override fun save(threshold: ConsumptionThreshold): ConsumptionThreshold {
                store[threshold.id] = threshold
                return threshold
            }

            override fun findByAccountAndWaterPoint(accountId: UUID, waterPointId: UUID): ConsumptionThreshold? =
                store.values.find { it.accountId == accountId && it.waterPointId == waterPointId }

            override fun findAllByAccountId(accountId: UUID): List<ConsumptionThreshold> =
                store.values.filter { it.accountId == accountId }
        }

        preferenceRepo = object : NotificationPreferenceRepository {
            private val store = mutableMapOf<UUID, NotificationPreference>()

            override fun save(preference: NotificationPreference): NotificationPreference {
                store[preference.accountId] = preference
                return preference
            }

            override fun findByAccountId(accountId: UUID): NotificationPreference? = store[accountId]
        }

        recipientRepo = object : AuthorizedAlertRecipientRepository {
            private val store = mutableMapOf<UUID, AuthorizedAlertRecipient>()

            override fun save(recipient: AuthorizedAlertRecipient): AuthorizedAlertRecipient {
                store[recipient.id] = recipient
                return recipient
            }

            override fun findById(id: UUID): AuthorizedAlertRecipient? = store[id]

            override fun findAllByAccountId(accountId: UUID): List<AuthorizedAlertRecipient> =
                store.values.filter { it.accountId == accountId }

            override fun findEnabledByAccountId(accountId: UUID): List<AuthorizedAlertRecipient> =
                store.values.filter { it.accountId == accountId && it.enabled }
        }

        handler = AlertManagementCommandHandler(alertRepo, thresholdRepo, preferenceRepo, recipientRepo)
    }

    // US17 - Escenario 1: Definición de un umbral propio
    @Test
    fun `should apply user defined threshold when custom limit is provided`() {
        val command = ConfigureThresholdCommand(
            accountId = UUID.randomUUID(),
            waterPointId = UUID.randomUUID(),
            period = "DAILY",
            customLimitLitersPerHour = BigDecimal("180.00"),
            baselineLitersPerHour = BigDecimal("100.00")
        )

        val result = handler.configureThreshold(command)

        assertEquals(ThresholdSource.USER_DEFINED, result.source)
        assertEquals(BigDecimal("180.00"), result.effectiveLimit())
    }

    // US17 - Escenario 2: Umbral automático a partir de la línea base
    @Test
    fun `should calculate automatic threshold from baseline when custom limit is null`() {
        val command = ConfigureThresholdCommand(
            accountId = UUID.randomUUID(),
            waterPointId = UUID.randomUUID(),
            period = "HOURLY",
            customLimitLitersPerHour = null,
            baselineLitersPerHour = BigDecimal("100.00")
        )

        val result = handler.configureThreshold(command)

        assertEquals(ThresholdSource.SYSTEM_CALCULATED, result.source)
        assertEquals(BigDecimal("125.00"), result.effectiveLimit())
    }

    // US18 - Escenario 2: Preferencias de notificación por nivel mínimo de urgencia
    @Test
    fun `should suppress push notification when alert urgency is below minimum configured`() {
        val accountId = UUID.randomUUID()
        val pref = handler.configureNotificationPreference(
            ConfigureNotificationPreferenceCommand(
                accountId = accountId,
                groupingWindowMinutes = 45,
                minimumUrgency = UrgencyLevel.CRITICAL
            )
        )

        assertFalse(pref.shouldSendPush(UrgencyLevel.INFORMATIONAL))
        assertFalse(pref.shouldSendPush(UrgencyLevel.MODERATE))
        assertTrue(pref.shouldSendPush(UrgencyLevel.CRITICAL))
    }

    // US19 - Escenario 1: Notificación a usuarios autorizados
    @Test
    fun `should notify only enabled authorized recipients on leak alert`() {
        val accountId = UUID.randomUUID()
        handler.addAuthorizedRecipient(
            AddAuthorizedRecipientCommand(accountId, "Carlos Perez", "carlos@qlic.pe", "token-1", true)
        )
        handler.addAuthorizedRecipient(
            AddAuthorizedRecipientCommand(accountId, "Ana Gomez", "ana@qlic.pe", "token-2", false)
        )

        val notified = handler.dispatchPushToAuthorizedUsers(accountId)

        assertEquals(1, notified.size)
        assertEquals("Carlos Perez", notified.first().fullName)
        assertNotNull(notified.first().lastNotifiedAt)
    }

    // US20 - Escenario 1: Cierre/Atención de una alerta activa registrando la acción
    @Test
    fun `should mark active alert as acknowledged and record action taken`() {
        val accountId = UUID.randomUUID()
        val activeAlert = alertRepo.save(
            LeakAlert(
                accountId = accountId,
                waterPointId = UUID.randomUUID(),
                urgency = UrgencyLevel.CRITICAL,
                estimatedVolumeLiters = BigDecimal("45.0"),
                estimatedCostSoles = BigDecimal("12.50"),
                recommendedAction = "Cerrar llave general"
            )
        )

        val updated = handler.acknowledgeAlert(
            AcknowledgeAlertCommand(
                alertId = activeAlert.id,
                accountId = accountId,
                actionTaken = "Se reemplazó el empaque de la válvula principal"
            )
        )

        assertEquals(AlertState.ACKNOWLEDGED, updated.state)
        assertEquals("Se reemplazó el empaque de la válvula principal", updated.acknowledgedAction)
    }
}