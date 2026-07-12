package com.mpcorp.attendance.auth.security

import com.mpcorp.attendance.common.exception.ErrorCode
import com.mpcorp.attendance.common.response.ApiError
import com.mpcorp.attendance.common.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

/** Returns a JSON [ApiResponse] with 401 when an unauthenticated request hits a
 *  protected endpoint (instead of Spring's default HTML/empty body). */
@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val body = ApiResponse.fail(
            ApiError(code = ErrorCode.UNAUTHORIZED.name, message = ErrorCode.UNAUTHORIZED.defaultMessage),
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
