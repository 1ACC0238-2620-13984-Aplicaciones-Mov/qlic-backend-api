package com.wasd.qlic.consumption.domain.repositories

import com.wasd.qlic.consumption.domain.model.PremisesActivitySchedule
import java.util.UUID

interface ActivityScheduleRepository {
    fun save(schedule: PremisesActivitySchedule): PremisesActivitySchedule
    fun findByAccountId(accountId: UUID): PremisesActivitySchedule?
}
