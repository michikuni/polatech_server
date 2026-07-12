package com.mpcorp.attendance

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Full-context smoke test. Disabled for now: it boots the whole application,
 * which requires a running MySQL 8 + Flyway (ddl-auto=validate). A test
 * database is out of scope for the common module (steps 1–6); this will be
 * re-enabled together with the first feature, backed by a proper test DB.
 */
@Disabled("Requires a running MySQL + Flyway; re-enable with the first feature's test DB setup")
@SpringBootTest
class AttendanceApplicationTests {

	@Test
	fun contextLoads() {
	}

}
