package com.wasd.qlic.monitoring.application.services

import com.wasd.qlic.monitoring.application.commands.RegisterConsumptionReadingCommand
import com.wasd.qlic.monitoring.application.handlers.RegisterConsumptionReadingCommandHandler
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.model.SimulationScenario
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Meter simulator for demonstrations and user validation without physical hardware. Every interval it registers a
 * reading for each device in simulation mode through the same command as a real meter, so the dashboard, the
 * reports, the automatic leak detection and the push notifications behave exactly as in production.
 * NORMAL follows a household or business daily profile; LEAK adds a continuous flow that exceeds the default threshold.
 */
@Service
class MeterSimulator(
    private val deviceRepository: IoTDeviceRepository,
    private val registerReadingHandler: RegisterConsumptionReadingCommandHandler,
    @Value("\${qlic.simulator.interval-ms:300000}") private val intervalMs: Long
) {
    private val log = LoggerFactory.getLogger(MeterSimulator::class.java)

    @Scheduled(
        initialDelayString = "\${qlic.simulator.initial-delay-ms:30000}",
        fixedDelayString = "\${qlic.simulator.interval-ms:300000}"
    )
    fun scheduledTick() {
        tick()
    }

    /** Registers one simulated reading per device in simulation mode; returns how many were registered. */
    fun tick(now: LocalDateTime = LocalDateTime.now(), random: Random = Random.Default): Int =
        deviceRepository.findSimulated()
            .filter { it.waterPointId != null }
            .count { device ->
                try {
                    simulateReading(device, now, random)
                    true
                } catch (exception: RuntimeException) {
                    log.warn("The simulated reading of device {} could not be registered", device.id, exception)
                    false
                }
            }

    private fun simulateReading(device: IoTDevice, now: LocalDateTime, random: Random) {
        val flowPerMinute = flowLitersPerMinute(device.simulationScenario ?: SimulationScenario.NORMAL, now.hour, random)
        val minutes = BigDecimal.valueOf(intervalMs).divide(MILLIS_PER_MINUTE, 3, RoundingMode.HALF_UP)
        val battery = (device.batteryPercentage - if (random.nextInt(12) == 0) 1 else 0).coerceIn(20, 100)
        registerReadingHandler.handle(
            RegisterConsumptionReadingCommand(
                deviceId = device.id,
                accountId = device.accountId,
                volumeLiters = flowPerMinute.multiply(minutes).setScale(3, RoundingMode.HALF_UP),
                recordedAt = now,
                flowRateLitersPerMinute = flowPerMinute,
                pressureBar = BigDecimal.valueOf(1.8 + random.nextDouble() * 0.6).setScale(2, RoundingMode.HALF_UP),
                batteryPercentage = battery
            )
        )
    }

    companion object {
        private val MILLIS_PER_MINUTE = BigDecimal("60000")

        /** Flow in liters per minute for the scenario and hour of the day. */
        fun flowLitersPerMinute(scenario: SimulationScenario, hour: Int, random: Random): BigDecimal {
            val usual = when (hour) {
                in 6..8, in 19..21 -> 0.6 + random.nextDouble() * 1.0 // morning and evening peaks
                in 9..18 -> 0.3 + random.nextDouble() * 0.6
                else -> random.nextDouble() * 0.1 // night: almost no consumption
            }
            val leak = if (scenario == SimulationScenario.LEAK) 4.0 + random.nextDouble() * 1.5 else 0.0
            return BigDecimal.valueOf(usual + leak).setScale(3, RoundingMode.HALF_UP)
        }
    }
}
