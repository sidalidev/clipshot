package dev.sidali.clipshot

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick Settings tile: pause or resume Clipshot without opening the app. Same state as the main switch. */
class PauseTile : TileService() {
    override fun onStartListening() { refresh() }

    override fun onClick() {
        MainSwitch.set(this, !Prefs.enabled(this))
        refresh()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val on = MainSwitch.running(Prefs.enabled(this), Shots.hasAccess(this))
        val label = getString(if (on) R.string.status_on else R.string.status_off)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = label
        tile.stateDescription = label
        tile.updateTile()
    }
}
