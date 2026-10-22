package dev.sidali.clipshot

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.MediaStore

/**
 * Woken by the system when the photo library changes: no background service, no persistent
 * notification, no battery cost while nothing happens. A content-trigger job fires once,
 * so it re-arms itself every time.
 */
class ShotJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            try {
                // The screenshot is first inserted as "pending", then published: retry briefly.
                repeat(6) { if (Shots.copyLatest(this)) return@Thread; Thread.sleep(250) }
            } finally {
                jobFinished(params, false)
                schedule(this)
            }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters) = false

    companion object {
        private const val ID = 4242

        fun schedule(ctx: Context) {
            val js = ctx.getSystemService(JobScheduler::class.java)
            if (!Prefs.enabled(ctx) || !Shots.hasAccess(ctx)) { js.cancel(ID); return }
            val info = JobInfo.Builder(ID, ComponentName(ctx, ShotJob::class.java))
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
                    ),
                )
                .setTriggerContentUpdateDelay(50)
                .setTriggerContentMaxDelay(500)
                .build()
            js.schedule(info)
        }

        fun armed(ctx: Context) = ctx.getSystemService(JobScheduler::class.java).getPendingJob(ID) != null
    }
}
