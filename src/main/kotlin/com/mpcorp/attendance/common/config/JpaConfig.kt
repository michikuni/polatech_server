package com.mpcorp.attendance.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/** Enables JPA auditing so [com.mpcorp.attendance.common.persistence.BaseEntity]
 *  timestamps (createdAt/updatedAt) are populated automatically. */
@Configuration
@EnableJpaAuditing
class JpaConfig
