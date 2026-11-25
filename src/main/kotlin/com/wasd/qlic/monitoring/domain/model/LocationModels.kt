package com.wasd.qlic.monitoring.domain.model

import java.time.Instant
import java.util.UUID

/** Kind of use of a Water Point, used to tailor the saving recommendations. */
enum class WaterPointType {
    KITCHEN,
    BATHROOM,
    LAUNDRY,
    GARDEN,
    TANK_INLET,
    MAIN_LINE,
    PRODUCTION,
    OTHER
}

/** Aggregate Root: premises or home of the subscriber that groups its Water Points (US13). */
class Location(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    name: String,
    address: String? = null,
    val createdAt: Instant = Instant.now()
) {
    var name: String = name.trim()
        private set
    var address: String? = address?.trim()?.ifBlank { null }
        private set

    init {
        require(this.name.isNotBlank()) { "The name of the location is required" }
    }

    fun rename(name: String, address: String?) {
        require(name.isNotBlank()) { "The name of the location is required" }
        this.name = name.trim()
        this.address = address?.trim()?.ifBlank { null }
    }
}

/** Entity: physical point of a location whose water consumption is monitored by a device. */
class WaterPoint(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    locationId: UUID,
    name: String,
    type: WaterPointType = WaterPointType.OTHER,
    val createdAt: Instant = Instant.now()
) {
    var locationId: UUID = locationId
        private set
    var name: String = name.trim()
        private set
    var type: WaterPointType = type
        private set

    init {
        require(this.name.isNotBlank()) { "The name of the Water Point is required" }
    }

    fun update(name: String, type: WaterPointType, locationId: UUID) {
        require(name.isNotBlank()) { "The name of the Water Point is required" }
        this.name = name.trim()
        this.type = type
        this.locationId = locationId
    }
}
