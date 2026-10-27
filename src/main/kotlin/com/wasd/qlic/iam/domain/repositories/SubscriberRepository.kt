package com.wasd.qlic.iam.domain.repositories

import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.EmailAddress

interface SubscriberRepository {
    fun save(subscriber: Subscriber): Subscriber
    fun findByEmail(email: EmailAddress): Subscriber?
    fun existsByEmail(email: EmailAddress): Boolean
}
