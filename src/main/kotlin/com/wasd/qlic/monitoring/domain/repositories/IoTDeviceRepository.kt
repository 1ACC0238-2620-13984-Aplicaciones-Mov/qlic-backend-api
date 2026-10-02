package com.wasd.qlic.monitoring.domain.repositories

import com.wasd.qlic.monitoring.domain.model.IoTDevice
import java.util.UUID

interface IoTDeviceRepository {
    fun save(device: IoTDevice): IoTDevice
    fun findById(id: UUID): IoTDevice?
    fun findBySerialNumber(serialNumber: String): IoTDevice?
    fun findByAccountId(accountId: UUID): List<IoTDevice>
}