package com.mpcorp.attendance.auth.config

import com.mpcorp.attendance.auth.security.JwtAuthenticationFilter
import com.mpcorp.attendance.auth.security.RestAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.HttpMethod
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: RestAuthenticationEntryPoint,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // Static admin portal (single-page UI) served from resources/static.
                it.requestMatchers(HttpMethod.GET, *STATIC_PORTAL).permitAll()
                it.requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
                it.requestMatchers("/api/admin/**").hasRole("ADMIN")
                it.anyRequest().denyAll()
            }
            .exceptionHandling { it.authenticationEntryPoint(authenticationEntryPoint) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    private companion object {
        // Device/employee flows authenticate by ECDSA signature, not JWT.
        val PUBLIC_ENDPOINTS = arrayOf(
            "/api/auth/login",
            "/api/devices/enroll",
            "/api/challenge",
            "/api/attendance",
            "/api/attendance/history",
        )

        // The portal is a single self-contained HTML page; the API calls it makes
        // still require a JWT (admin) or signature (device) as usual.
        val STATIC_PORTAL = arrayOf(
            "/",
            "/index.html",
            "/favicon.ico",
        )
    }
}
