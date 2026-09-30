package app.spotifycustomizer.patches.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class ProbeTest {
    @Test
    fun deliberatelyFails() {
        assertTrue(false, "proving the test harness gates the build")
    }
}
