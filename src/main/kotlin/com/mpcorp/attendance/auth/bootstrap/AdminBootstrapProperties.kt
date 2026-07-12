package com.mpcorp.attendance.auth.bootstrap

import org.springframework.boot.context.properties.ConfigurationProperties

/** Bootstrap credentials for the very first admin, bound from `app.bootstrap.admin.*`.
 *  [password] has no default — if unset, no admin is created. */
@ConfigurationProperties(prefix = "app.bootstrap.admin")
data class AdminBootstrapProperties(
    val username: String = "admin",
    val password: String? = null,
)
