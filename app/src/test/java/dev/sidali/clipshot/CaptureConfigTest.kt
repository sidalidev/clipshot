package dev.sidali.clipshot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The accessibility service must stay unable to see the screen. This test reads the declaration and
 * fails if anyone widens it.
 */
class CaptureConfigTest {
    private val ns = "http://schemas.android.com/apk/res/android"
    private val root = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(File("src/main/res/xml/capture_service.xml")).documentElement

    private fun attr(name: String): String? = root.getAttributeNS(ns, name).ifEmpty { null }

    @Test fun receivesNoEvent() = assertNull(attr("accessibilityEventTypes"))

    @Test fun cannotReadWindows() = assertEquals("false", attr("canRetrieveWindowContent"))

    @Test fun onlyAsksForTheButton() = assertEquals("flagRequestAccessibilityButton", attr("accessibilityFlags"))

    @Test fun noGesturesNoKeys() {
        assertEquals("false", attr("canPerformGestures"))
        assertEquals("false", attr("canRequestFilterKeyEvents"))
        assertEquals("false", attr("canRequestFingerprintGestures"))
        assertNull(attr("packageNames"))
        assertFalse(attr("isAccessibilityTool") == "true")
    }

    @Test fun playFlavorRemovesIt() {
        val play = File("src/play/AndroidManifest.xml").readText()
        for (c in listOf(".CaptureService", ".CaptureTile", ".CaptureSetupActivity")) {
            assertTrue("$c is not removed from play", Regex("""android:name="\Q$c\E" tools:node="remove"""").containsMatchIn(play))
        }
    }
}
