package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.model.aggregates.Account
import com.wasd.qlic.iam.domain.model.entities.Subscriber
import com.wasd.qlic.iam.domain.model.valueobjects.AccessToken
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel

/**
 * Domain port to issue access tokens. Implemented by JwtTokenProvider.
 */
interface TokenProvider {
    fun issue(subscriber: Subscriber, account: Account, permission: PermissionLevel = PermissionLevel.OWNER): AccessToken
}
