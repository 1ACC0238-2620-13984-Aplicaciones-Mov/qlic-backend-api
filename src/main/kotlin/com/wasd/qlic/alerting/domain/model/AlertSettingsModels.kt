package com.wasd.qlic.alerting.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

class ConsumptionThreshold(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val waterPointId: UUID,
    val period: String = "HOURLY",
    var customLimitLitersPerHour: BigDecimal? = null,
    val baselineLitersPerHour: BigDecimal = BigDecimal("100.00"),
    var source: ThresholdSource = if (customLimitLitersPerHour != null) ThresholdSource.USER_DEFINED else ThresholdSource.SYSTEM_CALCULATED,
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * US17 - Escenario 1 y 2:
     * Si el suscriptor define un umbral propio, usa ese valor (USER_DEFINED).
     * Si no lo define, calcula automáticamente +25% sobre la línea base (SYSTEM_CALCULATED).
     */
    fun effectiveLimit(): BigDecimal {
        return customLimitLitersPerHour
            ?: baselineLitersPerHour.multiply(BigDecimal("1.25")).setScale(2, RoundingMode.HALF_UP)
    }
}

class NotificationPreference(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    var groupingWindowMinutes: Int = 30,
    var minimumUrgency: UrgencyLevel = UrgencyLevel.INFORMATIONAL,
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * US18 - Escenario 2:
     * Verifica si la urgencia de la alerta alcanza el nivel mínimo configurado para enviar push.
     */
    fun shouldSendPush(alertUrgency: UrgencyLevel): Boolean {
        return alertUrgency.ordinal >= minimumUrgency.ordinal
    }
}

class AuthorizedAlertRecipient(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val fullName: String,
    val email: String,
    val devicePushToken: String,
    var enabled: Boolean = true,
    var lastNotifiedAt: LocalDateTime? = null
) {
    fun markNotified() {
        this.lastNotifiedAt = LocalDateTime.now()
    }
}