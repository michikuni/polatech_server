package com.mpcorp.attendance.common.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Uniform envelope for every REST response. Either [data] (on success) or
 * [error] (on failure) is present, never both.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<out T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
) {
    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun fail(error: ApiError): ApiResponse<Nothing> = ApiResponse(success = false, error = error)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiError(
    val code: String,
    val message: String,
    val fieldErrors: List<FieldErrorDetail>? = null,
)

data class FieldErrorDetail(
    val field: String,
    val message: String,
)
