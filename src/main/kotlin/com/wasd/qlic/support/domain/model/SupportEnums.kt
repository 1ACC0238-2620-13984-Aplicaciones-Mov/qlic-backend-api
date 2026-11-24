package com.wasd.qlic.support.domain.model

/**
 * Value Object: state of an incident (US37 - Business rule).
 * The valid transitions are PENDING -> VISIT_REQUESTED -> IN_PROGRESS -> RESOLVED;
 * an incident can also be resolved directly from PENDING or VISIT_REQUESTED.
 */
enum class IncidentState {
    PENDING,
    VISIT_REQUESTED,
    IN_PROGRESS,
    RESOLVED;

    fun canTransitionTo(target: IncidentState): Boolean = when (this) {
        PENDING -> target == VISIT_REQUESTED || target == RESOLVED
        VISIT_REQUESTED -> target == IN_PROGRESS || target == RESOLVED
        IN_PROGRESS -> target == RESOLVED
        RESOLVED -> false
    }
}

/** Value Object: origin of the incident (US35 - Scenario 2). */
enum class IncidentOrigin {
    SELF_REPORTED,
    ALERT_DERIVED
}

/** Value Object: state of a technical visit (US36). */
enum class VisitState {
    REQUESTED,
    ASSIGNED,
    COMPLETED,
    CANCELLED
}
