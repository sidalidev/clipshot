package dev.sidali.clipshot

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService

/**
 * The one switch that stops everything and starts it again. Off: the photo library trigger is
 * cancelled, pending gallery removals are dropped (those screenshots stay in your photos), and
 * nothing is armed again at boot until the user turns it back on. The tile and the main screen
 * both go through here, so they always show the same state.
 */
object MainSwitch {
    /** Clipshot is actually working only when it is on and can read screenshots. */
    fun running(enabled: Boolean, access: Boolean) = enabled && access

    fun set(ctx: Context, on: Boolean) {
        Prefs.setEnabled(ctx, on)
        ShotJob.schedule(ctx)
        if (!on) Cleanup.cancel(ctx)
        try {
            TileService.requestListeningState(ctx, ComponentName(ctx, PauseTile::class.java))
        } catch (_: Exception) {
            // The tile is not added: nothing to refresh.
        }
    }
}
