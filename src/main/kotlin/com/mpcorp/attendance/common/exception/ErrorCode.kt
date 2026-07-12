package com.mpcorp.attendance.common.exception

import org.springframework.http.HttpStatus

/**
 * Stable, machine-readable error codes returned to clients. The [name] of each
 * constant is the wire code; [httpStatus] is the HTTP status it maps to.
 */
enum class ErrorCode(val httpStatus: HttpStatus, val defaultMessage: String) {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    BUSINESS_RULE_VIOLATION(HttpStatus.CONFLICT, "Business rule violated"),
    INVALID_PUBLIC_KEY(HttpStatus.BAD_REQUEST, "Invalid public key"),
    INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "Signature verification failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error"),
}
