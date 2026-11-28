package com.wasd.qlic.monitoring.domain.repositories

import com.wasd.qlic.monitoring.domain.model.IoTDevice
import java.time.LocalDateTime
import java.util.UUID

interface IoTDeviceRepository {
    fun save(device: IoTDevice): IoTDevice
    fun findById(id: UUID): IoTDevice?
    fun findBySerialNumber(serialNumber: String): IoTDevice?
    fun findByAccountId(accountId: UUID): List<IoTDevice>
    fun existsByWaterPointId(waterPointId: UUID): Boolean
    /** Devices in simulation mode, which receive generated readings. */
    fun findSimulated(): List<IoTDevice>
    /** Devices not marked OFFLINE whose last synchronization is older than the limit. */
    fun findConnectedNotSyncedSince(limit: LocalDateTime): List<IoTDevice>
    fun delete(device: IoTDevice)
}
