package dev.sidali.clipshot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Content-trigger jobs do not survive a reboot or an update: re-arm. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = ShotJob.schedule(ctx)
}
