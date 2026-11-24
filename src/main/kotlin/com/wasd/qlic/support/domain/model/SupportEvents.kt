package com.wasd.qlic.support.domain.model

import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

/** Domain Event published when an incident is registered (US35). */
data class IncidentRegisteredEvent(
    val incidentId: UUID,
    val accountId: UUID,
    val origin: String,
    val alertId: UUID?,
    val occurredOn: Instant = Instant.now()
)

/** Domain Event published when a technician is assigned to a visit (US36 - Scenario 2). */
data class TechnicianAssignedEvent(
    val incidentId: UUID,
    val accountId: UUID,
    val technicianId: UUID,
    val scheduledAt: LocalDateTime,
    val occurredOn: Instant = Instant.now()
)

/** Domain Event published when an incident is resolved. Consumed by Alerting. */
data class IncidentResolvedEvent(
    val incidentId: UUID,
    val accountId: UUID,
    val alertId: UUID?,
    val occurredOn: Instant = Instant.now()
)
