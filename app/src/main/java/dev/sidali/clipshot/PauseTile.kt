package dev.sidali.clipshot

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: pause or resume Clipshot without opening the app. */
class PauseTile : TileService() {
    override fun onStartListening() { refresh() }

    override fun onClick() {
        Prefs.setEnabled(this, !Prefs.enabled(this))
        ShotJob.schedule(this)
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = Prefs.enabled(this) && Shots.hasAccess(this)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(if (on) R.string.status_on else R.string.status_off)
        tile.updateTile()
    }
}
