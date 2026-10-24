package dev.sidali.clipshot

import android.app.job.JobScheduler
import android.content.ClipboardManager
import android.content.Context

/**
 * "Erase everything": puts Clipshot back the way it was on first launch. Clipshot has no account, no
 * server and no key, so this is all local: settings and counters, the private copies, the pending
 * gallery removals, the clip it placed on the clipboard, and the Capture service if it is on.
 */
object Erase {
    fun everything(ctx: Context) {
        // Pending gallery removals and the screenshot trigger: Clipshot's own jobs only.
        ctx.getSystemService(JobScheduler::class.java).cancelAll()
        // Settings and counters. "Remove from gallery" goes back to off.
        Prefs.clear(ctx)
        // The last three copies, and anything else the app wrote.
        Shots.dir(ctx).deleteRecursively()
        ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        // Our clip would now point to a deleted file: take it off the clipboard if it is still there.
        runCatching {
            val cm = ctx.getSystemService(ClipboardManager::class.java)
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0 && clip.getItemAt(0).uri?.authority == Shots.AUTHORITY) cm.clearPrimaryClip()
        }
        // Capture is off on first launch. The service can turn itself off; only the user can turn it on.
        if (BuildConfig.CAPTURE) CaptureService.running()?.disableSelf()
        // On by default, as on first launch (if the read permission is still granted).
        ShotJob.schedule(ctx)
    }
}
