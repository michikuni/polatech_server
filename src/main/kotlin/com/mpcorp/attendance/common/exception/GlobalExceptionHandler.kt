package com.mpcorp.attendance.common.exception

import com.mpcorp.attendance.common.crypto.CryptoException
import com.mpcorp.attendance.common.crypto.InvalidPublicKeyException
import com.mpcorp.attendance.common.crypto.SignatureVerificationException
import com.mpcorp.attendance.common.response.ApiError
import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.common.response.FieldErrorDetail
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(ex: ApiException): ResponseEntity<ApiResponse<Nothing>> =
        respond(ex.errorCode, ex.message)

    @ExceptionHandler(CryptoException::class)
    fun handleCryptoException(ex: CryptoException): ResponseEntity<ApiResponse<Nothing>> {
        val errorCode = when (ex) {
            is InvalidPublicKeyException -> ErrorCode.INVALID_PUBLIC_KEY
            is SignatureVerificationException -> ErrorCode.INVALID_SIGNATURE
        }
        return respond(errorCode, ex.message ?: errorCode.defaultMessage)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            FieldErrorDetail(field = it.field, message = it.defaultMessage ?: "invalid")
        }
        val error = ApiError(
            code = ErrorCode.VALIDATION_ERROR.name,
            message = ErrorCode.VALIDATION_ERROR.defaultMessage,
            fieldErrors = fieldErrors,
        )
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.httpStatus).body(ApiResponse.fail(error))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException): ResponseEntity<ApiResponse<Nothing>> {
        log.debug("No static resource for {}", ex.resourcePath)
        return respond(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.defaultMessage)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("Unhandled exception", ex)
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage)
    }

    private fun respond(errorCode: ErrorCode, message: String): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(errorCode.httpStatus)
            .body(ApiResponse.fail(ApiError(code = errorCode.name, message = message)))
}
