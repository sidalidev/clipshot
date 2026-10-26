package dev.sidali.clipshot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Content-trigger jobs do not survive a reboot or an update: re-arm, unless the user paused Clipshot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        ShotJob.schedule(ctx)
        if (Prefs.enabled(ctx) && Prefs.pendingDeletes(ctx).isNotEmpty()) Cleanup.arm(ctx, 5_000L)
    }
}
