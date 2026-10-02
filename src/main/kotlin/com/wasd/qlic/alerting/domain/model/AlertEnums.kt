package com.wasd.qlic.alerting.domain.model

enum class AlertState {
    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED
}

enum class UrgencyLevel {
    INFORMATIONAL,
    MODERATE,
    CRITICAL
}

enum class ThresholdSource {
    USER_DEFINED,
    SYSTEM_CALCULATED
}