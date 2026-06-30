package com.mpcorp.attendance.common.crypto

import org.springframework.stereotype.Component
import java.security.SecureRandom

interface ChallengeGenerator {

    fun generate(): ByteArray

}

@Component
class SecureRandomChallengeGenerator : ChallengeGenerator {

    private val secureRandom = SecureRandom()

    override fun generate(): ByteArray {
        val challenge = ByteArray(CryptoConstants.CHALLENGE_SIZE)
        secureRandom.nextBytes(challenge)
        return challenge
    }
}