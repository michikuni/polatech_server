package com.mpcorp.attendance.common.exception

/**
 * Base type for expected, mappable business errors. Carries an [ErrorCode] that
 * the Global Exception Handler turns into an HTTP status + wire code.
 */
open class ApiException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
) : RuntimeException(message)

class ResourceNotFoundException(
    message: String = ErrorCode.RESOURCE_NOT_FOUND.defaultMessage,
) : ApiException(ErrorCode.RESOURCE_NOT_FOUND, message)

class BusinessRuleException(
    message: String,
) : ApiException(ErrorCode.BUSINESS_RULE_VIOLATION, message)

class UnauthorizedException(
    message: String = ErrorCode.UNAUTHORIZED.defaultMessage,
) : ApiException(ErrorCode.UNAUTHORIZED, message)

class InvalidCredentialsException(
    message: String = ErrorCode.INVALID_CREDENTIALS.defaultMessage,
) : ApiException(ErrorCode.INVALID_CREDENTIALS, message)

class InvalidSignatureException(
    message: String = ErrorCode.INVALID_SIGNATURE.defaultMessage,
) : ApiException(ErrorCode.INVALID_SIGNATURE, message)
