package dev.sidali.clipshot

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

/*
 * "Capture": take a screenshot from a Quick Settings tile or the floating accessibility button.
 * Full flavor only (the play flavor removes these components from its manifest).
 *
 * Android only lets an app take a screenshot through an accessibility service. This one is as narrow
 * as Android allows (res/xml/capture_service.xml): it subscribes to no event, cannot retrieve window
 * content, cannot perform gestures and does not filter keys. Its one call is
 * performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT), the same as pressing the two buttons. The
 * screenshot is then saved by the system and copied by the usual path (ShotJob).
 */
class CaptureService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private val button = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) = capture(BUTTON_DELAY_MS)
    }

    override fun onServiceConnected() {
        current = WeakReference(this)
        accessibilityButtonController.registerAccessibilityButtonCallback(button, main)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(button)
        main.removeCallbacksAndMessages(null)
        current = null
        return super.onUnbind(intent)
    }

    /** Never called: the service subscribes to no event. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    /** Presses the screenshot button after [delayMs], so whatever was just tapped is gone from the screen. */
    fun capture(delayMs: Long) {
        main.removeCallbacksAndMessages(null)
        main.postDelayed({ performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) }, delayMs)
    }

    /** From the tile: close the Quick Settings panel first, so it is not on the screenshot. */
    @SuppressLint("MissingPermission")
    fun captureAfterClosingPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            // Android 11 has no global action for this; the broadcast still closes the panel there.
            @Suppress("DEPRECATION") sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
        capture(PANEL_DELAY_MS)
    }

    companion object {
        /** Time for the panel to slide away before the screenshot. */
        private const val PANEL_DELAY_MS = 700L
        /** Time for the floating button's menu to close, when it was open. */
        private const val BUTTON_DELAY_MS = 300L

        @Volatile private var current: WeakReference<CaptureService>? = null

        /** The running service, or null when the user has not turned it on. */
        fun running(): CaptureService? = current?.get()

        /** True when the service is on in the system settings (it may take a moment to connect). */
        fun isEnabled(ctx: Context): Boolean {
            val me = ComponentName(ctx, CaptureService::class.java)
            val list = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return list.split(':').any { ComponentName.unflattenFromString(it) == me }
        }
    }
}

/** Quick Settings tile "Capture": closes the panel and takes a screenshot. */
class CaptureTile : TileService() {
    override fun onStartListening() {
        val tile = qsTile ?: return
        val ready = CaptureService.running() != null
        tile.state = Tile.STATE_INACTIVE
        tile.subtitle = if (ready) null else getString(R.string.capture_tile_setup)
        tile.updateTile()
    }

    // The Intent form is only used below Android 14, where it is the only one.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val service = CaptureService.running()
        if (service != null) { service.captureAfterClosingPanel(); return }
        // Not turned on yet: explain why it is needed, with a way to the setting.
        val intent = Intent(this, CaptureSetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION") startActivityAndCollapse(intent)
        }
    }
}
