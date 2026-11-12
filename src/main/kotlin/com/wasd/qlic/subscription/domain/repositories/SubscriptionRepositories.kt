package com.wasd.qlic.subscriptions.domain.repositories

import com.wasd.qlic.subscriptions.domain.model.PaymentRecord
import com.wasd.qlic.subscriptions.domain.model.Subscription
import java.util.UUID

interface SubscriptionRepository {
    fun save(subscription: Subscription): Subscription
    fun findById(id: UUID): Subscription?
    fun findByAccountId(accountId: UUID): Subscription?
}

interface PaymentRecordRepository {
    fun save(payment: PaymentRecord): PaymentRecord
    fun findAllByAccountId(accountId: UUID): List<PaymentRecord>
}