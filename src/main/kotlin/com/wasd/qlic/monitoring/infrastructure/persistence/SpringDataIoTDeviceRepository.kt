package com.wasd.qlic.monitoring.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataIoTDeviceRepository : JpaRepository<IoTDeviceEntity, UUID> {
    fun findBySerialNumber(serialNumber: String): IoTDeviceEntity?
    fun findByAccountId(accountId: UUID): List<IoTDeviceEntity>
}