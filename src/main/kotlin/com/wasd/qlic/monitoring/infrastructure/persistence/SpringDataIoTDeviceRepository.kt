package com.wasd.qlic.monitoring.infrastructure.persistence

import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SpringDataIoTDeviceRepository : JpaRepository<IoTDeviceEntity, UUID> {
    fun findBySerialNumber(serialNumber: String): IoTDeviceEntity?
    fun findByAccountId(accountId: UUID): List<IoTDeviceEntity>
    fun existsByWaterPointId(waterPointId: UUID): Boolean
    fun findBySimulationScenarioIsNotNull(): List<IoTDeviceEntity>
    fun findByStatusNotAndLastSyncAtBefore(status: DeviceStatus, limit: LocalDateTime): List<IoTDeviceEntity>
}