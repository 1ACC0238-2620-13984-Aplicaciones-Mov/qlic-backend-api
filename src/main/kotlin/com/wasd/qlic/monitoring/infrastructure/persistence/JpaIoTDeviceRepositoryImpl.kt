package com.wasd.qlic.monitoring.infrastructure.persistence

import com.wasd.qlic.monitoring.domain.model.IoTDevice
import com.wasd.qlic.monitoring.domain.repositories.IoTDeviceRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaIoTDeviceRepositoryImpl(
    private val jpaRepository: SpringDataIoTDeviceRepository
) : IoTDeviceRepository {

    override fun save(device: IoTDevice): IoTDevice {
        val entity = IoTDeviceEntity(
            id = device.id,
            serialNumber = device.serialNumber,
            model = device.model,
            qrCodePayload = device.qrCodePayload,
            accountId = device.accountId,
            waterPointId = device.waterPointId,
            status = device.status,
            batteryPercentage = device.batteryPercentage,
            lastSyncAt = device.lastSyncAt
        )
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: UUID): IoTDevice? {
        return jpaRepository.findById(id).map { toDomain(it) }.orElse(null)
    }

    override fun findBySerialNumber(serialNumber: String): IoTDevice? {
        return jpaRepository.findBySerialNumber(serialNumber)?.let { toDomain(it) }
    }

    override fun findByAccountId(accountId: UUID): List<IoTDevice> {
        return jpaRepository.findByAccountId(accountId).map { toDomain(it) }
    }

    private fun toDomain(entity: IoTDeviceEntity): IoTDevice {
        return IoTDevice(
            id = entity.id,
            serialNumber = entity.serialNumber,
            model = entity.model,
            qrCodePayload = entity.qrCodePayload,
            accountId = entity.accountId,
            waterPointId = entity.waterPointId,
            status = entity.status,
            batteryPercentage = entity.batteryPercentage,
            lastSyncAt = entity.lastSyncAt
        )
    }
}