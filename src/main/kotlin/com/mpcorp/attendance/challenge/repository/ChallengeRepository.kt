package com.mpcorp.attendance.challenge.repository

import com.mpcorp.attendance.challenge.entity.Challenge
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface ChallengeRepository : JpaRepository<Challenge, Long> {

    /**
     * Atomically claims an unconsumed challenge. Returns 1 if this call won the
     * claim, 0 if it was already consumed (race / replay) — giving exactly-once use.
     */
    @Modifying
    @Query("update Challenge c set c.consumedAt = :now where c.id = :id and c.consumedAt is null")
    fun consume(@Param("id") id: Long, @Param("now") now: Instant): Int
}
