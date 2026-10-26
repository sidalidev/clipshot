package dev.sidali.clipshot

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log

/**
 * Optional "clipboard only" mode: one minute after the copy, the original screenshot is removed from
 * the gallery. The wait leaves time for the system screenshot preview (edit, share) to finish with it.
 * Our private copy stays on the clipboard. Requires "All files access", asked only when the user turns it on.
 */
class Cleanup : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            try { run(this) } finally { jobFinished(params, false) }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters) = true

    companion object {
        private const val ID = 4243
        private const val DELAY_MS = 60_000L

        fun later(ctx: Context, mediaId: Long, addedAt: Long) {
            Prefs.addPendingDelete(ctx, mediaId, System.currentTimeMillis() + DELAY_MS, addedAt)
            arm(ctx, DELAY_MS)
        }

        fun arm(ctx: Context, delay: Long) {
            val info = JobInfo.Builder(ID, ComponentName(ctx, Cleanup::class.java))
                .setMinimumLatency(delay).setOverrideDeadline(delay + 30_000L).build()
            ctx.getSystemService(JobScheduler::class.java).schedule(info)
        }

        /** Paused: stop the timer and forget pending removals. Those screenshots stay in the gallery. */
        fun cancel(ctx: Context) {
            ctx.getSystemService(JobScheduler::class.java).cancel(ID)
            Prefs.clearPendingDeletes(ctx)
        }

        fun run(ctx: Context) {
            val now = System.currentTimeMillis()
            val pending = Prefs.pendingDeletes(ctx)
            for ((id, entry) in pending) {
                val (due, addedAt) = entry
                if (due > now) continue
                if (Prefs.removeFromGallery(ctx) && Shots.canDelete() && stillOurs(ctx, id, addedAt)) {
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    try { ctx.contentResolver.delete(uri, null, null) } catch (t: Throwable) { Log.w("Clipshot", "delete failed", t) }
                }
                Prefs.removePendingDelete(ctx, id)
            }
            val next = Prefs.pendingDeletes(ctx).values.minOfOrNull { it.first }
            if (next != null) arm(ctx, (next - now).coerceAtLeast(1_000L))
        }

        /**
         * Re-check right before deleting: same row, still a system screenshot, same date. If the user
         * moved, renamed or edited it in the meantime, or the id now points to something else, keep it.
         */
        private fun stillOurs(ctx: Context, id: Long, addedAt: Long): Boolean {
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            val proj = arrayOf(
                MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.OWNER_PACKAGE_NAME, MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
            )
            ctx.contentResolver.query(uri, proj, null, null)?.use { c ->
                if (!c.moveToFirst()) return false
                return Rules.mayDelete(c.getString(0), c.getString(1), c.getString(2)) &&
                    c.getLong(3) == addedAt && c.getLong(4) - addedAt < 30
            }
            return false
        }
    }
}
