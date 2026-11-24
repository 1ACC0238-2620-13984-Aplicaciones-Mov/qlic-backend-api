package com.wasd.qlic.shared.domain.exceptions

/** A requested resource does not exist or does not belong to the requester's account (HTTP 404). */
abstract class ResourceNotFoundException(messageKey: String, vararg arguments: Any) :
    DomainException(messageKey, *arguments)

/** The requester is authenticated but lacks the permission required by the operation (HTTP 403). */
abstract class OperationNotPermittedException(messageKey: String, vararg arguments: Any) :
    DomainException(messageKey, *arguments)

/** The operation conflicts with the current state of the resource (HTTP 409). */
abstract class BusinessConflictException(messageKey: String, vararg arguments: Any) :
    DomainException(messageKey, *arguments)
