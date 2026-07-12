package com.mpcorp.attendance.common.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ChallengeGeneratorTest {

    private val generator: ChallengeGenerator = SecureRandomChallengeGenerator()

    @Test
    fun `challenge has the configured length`() {
        assertEquals(CryptoConstants.CHALLENGE_SIZE, generator.generate().size)
    }

    @Test
    fun `every challenge is exactly 32 bytes`() {
        repeat(50) {
            assertEquals(32, generator.generate().size)
        }
    }

    @Test
    fun `two consecutive challenges are different`() {
        val first = generator.generate()
        val second = generator.generate()
        assertFalse(first.contentEquals(second), "Consecutive challenges must not be identical")
    }

    @Test
    fun `a large batch of challenges contains no duplicates`() {
        val count = 1_000
        val seen = HashSet<String>(count)
        repeat(count) {
            seen.add(Base64Utils.encode(generator.generate()))
        }
        assertEquals(count, seen.size, "All generated challenges must be unique")
    }
}
