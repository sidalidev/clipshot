package dev.sidali.clipshot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainSwitchTest {
    @Test fun runsOnlyWhenOnAndAllowed() {
        assertTrue(MainSwitch.running(enabled = true, access = true))
        assertFalse(MainSwitch.running(enabled = false, access = true))
        assertFalse(MainSwitch.running(enabled = true, access = false))
        assertFalse(MainSwitch.running(enabled = false, access = false))
    }
}
