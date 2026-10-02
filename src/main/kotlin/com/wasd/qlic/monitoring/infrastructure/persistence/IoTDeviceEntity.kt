package com.wasd.qlic.monitoring.infrastructure.persistence

import com.wasd.qlic.monitoring.domain.model.DeviceStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "iot_devices")
class IoTDeviceEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "serial_number", nullable = false, unique = true, length = 60)
    val serialNumber: String,

    @Column(nullable = false, length = 50)
    val model: String,

    @Column(name = "qr_payload", nullable = false, length = 255)
    val qrCodePayload: String,

    @Column(name = "account_id", nullable = false)
    val accountId: UUID,

    @Column(name = "water_point_id")
    var waterPointId: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DeviceStatus,

    @Column(name = "battery_percentage", nullable = false)
    var batteryPercentage: Int,

    @Column(name = "last_sync_at", nullable = false)
    var lastSyncAt: LocalDateTime
)