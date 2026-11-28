package com.wasd.qlic.shared.infrastructure.persistence

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * Hibernate creates the enum columns with a CHECK constraint listing the values known when the table was created,
 * and ddl-auto=update does not refresh it. When an enum gains a value, the stale constraint of an existing
 * PostgreSQL database rejects it, so it is dropped at startup; the enum itself still validates the values.
 * Statements that do not apply to the database (H2 names its constraints differently) are ignored.
 */
@Component
class EnumCheckConstraintMaintenance(private val jdbcTemplate: JdbcTemplate) {

    private val log = LoggerFactory.getLogger(EnumCheckConstraintMaintenance::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun dropStaleEnumChecks() {
        STALE_CHECKS.forEach { (table, constraint) ->
            try {
                jdbcTemplate.execute("ALTER TABLE IF EXISTS $table DROP CONSTRAINT IF EXISTS $constraint")
            } catch (exception: RuntimeException) {
                log.debug("Constraint {} of {} not dropped: {}", constraint, table, exception.message)
            }
        }
    }

    private companion object {
        /** push_deliveries.category gained DEVICE_OFFLINE in v1.5.0. */
        val STALE_CHECKS = listOf("push_deliveries" to "push_deliveries_category_check")
    }
}
