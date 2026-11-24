package com.wasd.qlic.monitoring.application.handlers

import com.wasd.qlic.monitoring.application.commands.RegisterConsumptionReadingCommand
import com.wasd.qlic.monitoring.domain.exceptions.DeviceWithoutWaterPointException
import com.wasd.qlic.monitoring.domain.exceptions.ReadingDeviceNotFoundException
import com.wasd.qlic.monitoring.domain.model.ConsumptionReading
import com.wasd.qlic.monitoring.domain.repositories.ConsumptionReadingRepository
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** Registers a reading of a device of the account under its current Water Point. */
@Component
class RegisterConsumptionReadingCommandHandler(
    private val deviceRepository: IoTDeviceRepository,
    private val readingRepository: ConsumptionReadingRepository
) {
    @Transactional
    fun handle(command: RegisterConsumptionReadingCommand): ConsumptionReading {
        val device = deviceRepository.findById(command.deviceId)?.takeIf { it.accountId == command.accountId }
            ?: throw ReadingDeviceNotFoundException()
        val waterPointId = device.waterPointId ?: throw DeviceWithoutWaterPointException()
        return readingRepository.save(
            ConsumptionReading(
                accountId = device.accountId,
                deviceId = device.id,
                waterPointId = waterPointId,
                volumeLiters = command.volumeLiters,
                recordedAt = command.recordedAt ?: LocalDateTime.now()
            )
        )
    }
}
