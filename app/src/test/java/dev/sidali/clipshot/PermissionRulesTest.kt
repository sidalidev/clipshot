package dev.sidali.clipshot

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionRulesTest {
    @Test fun rowsFollowTheBuild() {
        assertEquals(listOf(Access.PHOTOS), PermissionRules.rows(removeFromGallery = false, capture = false))
        assertEquals(listOf(Access.PHOTOS, Access.ALL_FILES, Access.CAPTURE), PermissionRules.rows(removeFromGallery = true, capture = true))
        assertEquals(
            PermissionRules.rows(BuildConfig.REMOVE_FROM_GALLERY, BuildConfig.CAPTURE).size,
            1 + (if (BuildConfig.REMOVE_FROM_GALLERY) 1 else 0) + (if (BuildConfig.CAPTURE) 1 else 0),
        )
    }

    @Test fun anythingOnOpensTheSettingsWhereItTurnsOff() {
        for (a in Access.values()) for (asked in listOf(true, false)) for (again in listOf(true, false))
            assertEquals(Tap.OPEN_SETTINGS, PermissionRules.tap(a, granted = true, askedBefore = asked, canAskAgain = again))
    }

    @Test fun photosOffAsksWhileAndroidStillShowsTheDialog() {
        assertEquals(Tap.ASK, PermissionRules.tap(Access.PHOTOS, granted = false, askedBefore = false, canAskAgain = false))
        assertEquals(Tap.ASK, PermissionRules.tap(Access.PHOTOS, granted = false, askedBefore = true, canAskAgain = true))
    }

    @Test fun photosRefusedForGoodOpensTheSettingsInsteadOfAskingAgain() =
        assertEquals(Tap.OPEN_SETTINGS, PermissionRules.tap(Access.PHOTOS, granted = false, askedBefore = true, canAskAgain = false))

    @Test fun specialAccessesAreOnlyInSettings() {
        for (a in listOf(Access.ALL_FILES, Access.CAPTURE))
            assertEquals(Tap.OPEN_SETTINGS, PermissionRules.tap(a, granted = false, askedBefore = false, canAskAgain = true))
    }
}
