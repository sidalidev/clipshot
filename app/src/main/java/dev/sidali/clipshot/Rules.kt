package dev.sidali.clipshot

/** Pure rules, kept free of Android types so they are unit-tested on the JVM. */
object Rules {
    /** Apps that save system screenshots (MediaStore OWNER_PACKAGE_NAME), AOSP/GrapheneOS first, then common OEMs. */
    val SCREENSHOT_OWNERS = setOf(
        "com.android.systemui",
        "com.samsung.android.app.smartcapture", "com.miui.screenshot", "com.oplus.screenshot",
        "com.coloros.screenshot", "com.oneplus.screenshot", "com.huawei.hwmultiscreenshot",
    )
    private val STANDARD_DIRS = setOf("pictures/screenshots/", "dcim/screenshots/")

    private fun looksLikeScreenshot(relativePath: String?, displayName: String?): Boolean {
        val path = relativePath.orEmpty().lowercase()
        val name = displayName.orEmpty().lowercase()
        return path.split('/').any { it == "screenshots" } || name.startsWith("screenshot")
    }

    /**
     * Copy only real system screenshots: saved by a known screenshot app, or, when the owner is unknown
     * (an OEM we do not list), saved in the standard Screenshots folder. A download named "Screenshot…"
     * or a synced folder from another device does not count.
     */
    fun isScreenshot(relativePath: String?, displayName: String?, owner: String?): Boolean {
        if (!looksLikeScreenshot(relativePath, displayName)) return false
        if (owner != null && owner.lowercase() in SCREENSHOT_OWNERS) return true
        return owner == null && relativePath.orEmpty().lowercase() in STANDARD_DIRS
    }

    /** Deleting is stricter: only files the system screenshot app itself saved. */
    fun mayDelete(relativePath: String?, displayName: String?, owner: String?): Boolean =
        owner != null && owner.lowercase() in SCREENSHOT_OWNERS && looksLikeScreenshot(relativePath, displayName)

    /** Names we hand out through ShotProvider: "shot-<digits>.<png|jpg|webp>", nothing else. */
    private val SAFE = Regex("""shot-\d{1,19}\.(png|jpg|webp)""")
    fun isSafeName(name: String?): Boolean = name != null && SAFE.matches(name)

    fun extensionFor(mime: String?): String = when (mime) {
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        else -> "png"
    }

    fun mimeFor(name: String): String = when (name.substringAfterLast('.')) {
        "jpg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> "image/png"
    }
}
