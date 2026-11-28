package com.wasd.qlic.monitoring.application.services

import com.wasd.qlic.monitoring.domain.model.DeviceDisconnectedEvent
import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * DeviceHealthService job: every few minutes marks as OFFLINE the devices that have not reported within the
 * disconnection limit and publishes DeviceDisconnectedEvent once per disconnection. The next reading of the device
 * brings it back ONLINE.
 */
@Service
class DeviceConnectivityMonitor(
    private val deviceRepository: IoTDeviceRepository,
    private val eventPublisher: ApplicationEventPublisher
) {

    @Scheduled(
        initialDelayString = "\${qlic.monitoring.connectivity-check.initial-delay-ms:60000}",
        fixedDelayString = "\${qlic.monitoring.connectivity-check.interval-ms:300000}"
    )
    fun scheduledCheck() {
        checkConnectivity()
    }

    /** Returns the devices that were disconnected in this run. */
    @Transactional
    fun checkConnectivity(now: LocalDateTime = LocalDateTime.now()): List<IoTDevice> {
        val limit = now.minusMinutes(IoTDevice.DISCONNECTION_LIMIT_MINUTES)
        return deviceRepository.findConnectedNotSyncedSince(limit)
            .filter { it.markDisconnectedIfSilent(now) }
            .map { device ->
                deviceRepository.save(device).also {
                    eventPublisher.publishEvent(
                        DeviceDisconnectedEvent(
                            accountId = it.accountId,
                            deviceId = it.id,
                            deviceName = it.name ?: it.serialNumber,
                            lastSyncAt = it.lastSyncAt
                        )
                    )
                }
            }
    }
}
