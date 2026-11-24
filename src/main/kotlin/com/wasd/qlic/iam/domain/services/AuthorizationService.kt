package com.wasd.qlic.iam.domain.services

import com.wasd.qlic.iam.domain.exceptions.PermissionDeniedException
import com.wasd.qlic.iam.domain.model.valueobjects.PermissionLevel
import org.springframework.stereotype.Service

/**
 * Domain Service: determines whether a user has the permission required over an account (US08).
 */
@Service
class AuthorizationService {

    /** US08 - Scenario 2: a VIEWER cannot execute modification operations. */
    fun requireModification(permission: PermissionLevel) {
        if (!permission.canModify()) throw PermissionDeniedException()
    }

    /** Only the account holder manages the authorized users and the account data. */
    fun requireOwner(permission: PermissionLevel) {
        if (permission != PermissionLevel.OWNER) throw PermissionDeniedException()
    }
}
