package com.wasd.qlic.shared.infrastructure.scheduling

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Enables the periodic jobs of the API: the meter simulator and the connectivity check of the devices.
 * Set qlic.scheduling.enabled=false to turn them off (the automated tests run them on demand).
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = ["qlic.scheduling.enabled"], havingValue = "true", matchIfMissing = true)
class SchedulingConfiguration
