package dev.sidali.clipshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private val sys = "com.android.systemui"

    @Test fun systemScreenshots() {
        assertTrue(Rules.isScreenshot("Pictures/Screenshots/", "Screenshot_20261022-130619.png", sys))
        assertTrue(Rules.isScreenshot("DCIM/Screenshots/", "Screenshot_1.jpg", "com.samsung.android.app.smartcapture"))
        // Unknown OEM: accepted only in the standard folder.
        assertTrue(Rules.isScreenshot("Pictures/Screenshots/", "Screenshot_1.png", null))
    }

    @Test fun lookalikesIgnored() {
        assertFalse(Rules.isScreenshot("Download/", "Screenshot 2026-10-22.png", "com.android.chrome"))
        assertFalse(Rules.isScreenshot("Download/", "Screenshot 2026-10-22.png", null))
        assertFalse(Rules.isScreenshot("Pictures/Screenshots/", "Screenshot_1.png", "com.nextcloud.client"))
        assertFalse(Rules.isScreenshot("Sync/Screenshots/", "Screenshot_1.png", null))
        assertFalse(Rules.isScreenshot("DCIM/Camera/", "PXL_20261022.jpg", "org.grapheneos.camera"))
        assertFalse(Rules.isScreenshot(null, null, null))
    }

    @Test fun deletionIsStricter() {
        assertTrue(Rules.mayDelete("Pictures/Screenshots/", "Screenshot_1.png", sys))
        assertFalse(Rules.mayDelete("Pictures/Screenshots/", "Screenshot_1.png", null))
        assertFalse(Rules.mayDelete("Pictures/Screenshots/", "Screenshot_1.png", "com.nextcloud.client"))
        assertFalse(Rules.mayDelete("Pictures/Holidays/", "IMG_1.png", sys))
    }

    @Test fun safeNamesOnly() {
        assertTrue(Rules.isSafeName("shot-17.png"))
        assertTrue(Rules.isSafeName("shot-123456789.jpg"))
        assertFalse(Rules.isSafeName("../shared_prefs/clipshot.xml"))
        assertFalse(Rules.isSafeName("shot-17.png/../../x"))
        assertFalse(Rules.isSafeName("shot-17.png.part"))
        assertFalse(Rules.isSafeName("shot-abc.png"))
        assertFalse(Rules.isSafeName(""))
        assertFalse(Rules.isSafeName(null))
    }

    @Test fun mimeRoundTrip() {
        for (m in listOf("image/png", "image/jpeg", "image/webp")) assertEquals(m, Rules.mimeFor("shot-1.${Rules.extensionFor(m)}"))
        assertEquals("png", Rules.extensionFor(null))
    }
}
