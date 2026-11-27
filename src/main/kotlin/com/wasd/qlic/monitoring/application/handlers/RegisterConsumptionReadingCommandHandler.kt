package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.RegisterConsumptionReadingCommand
import com.wasd.qlic.monitoring.domain.exceptions.DeviceWithoutWaterPointException
import com.wasd.qlic.monitoring.domain.exceptions.ReadingDeviceNotFoundException
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.model.ConsumptionReadingRegisteredEvent
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Registers a reading of a device of the account under its current Water Point. The reading also
 * refreshes the connection status and, when reported, the battery of the device, and publishes
 * ConsumptionReadingRegisteredEvent so Alerting evaluates it automatically (US15).
 */
@Component
class RegisterConsumptionReadingCommandHandler(
    private val deviceRepository: IoTDeviceRepository,
    private val readingRepository: ConsumptionReadingRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun handle(command: RegisterConsumptionReadingCommand): ConsumptionReading {
        val device = deviceRepository.findById(command.deviceId)?.takeIf { it.accountId == command.accountId }
            ?: throw ReadingDeviceNotFoundException()
        val waterPointId = device.waterPointId ?: throw DeviceWithoutWaterPointException()

        device.registerReading(command.batteryPercentage)
        deviceRepository.save(device)

        val recordedAt = command.recordedAt ?: LocalDateTime.now()
        val previous = readingRepository.findLatestByWaterPointIdBefore(waterPointId, recordedAt)
        val saved = readingRepository.save(
            ConsumptionReading(
                accountId = device.accountId,
                deviceId = device.id,
                waterPointId = waterPointId,
                volumeLiters = command.volumeLiters,
                recordedAt = recordedAt,
                flowRateLitersPerMinute = command.flowRateLitersPerMinute,
                pressureBar = command.pressureBar
            )
        )

        val recentPrevious = previous?.takeIf {
            !it.recordedAt.isBefore(recordedAt.minusMinutes(ConsumptionReading.SUSTAINED_WINDOW_MINUTES))
        }
        eventPublisher.publishEvent(
            ConsumptionReadingRegisteredEvent(
                accountId = saved.accountId,
                deviceId = saved.deviceId,
                waterPointId = saved.waterPointId,
                readingId = saved.id,
                recordedAt = saved.recordedAt,
                flowLitersPerHour = saved.flowLitersPerHour(previous),
                previousFlowLitersPerHour = recentPrevious?.flowLitersPerHour()
            )
        )
        return saved
    }
}
