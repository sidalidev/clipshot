package dev.sidali.clipshot

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/** Finds the screenshot the system just saved and puts a private copy of it on the clipboard. */
object Shots {
    private const val TAG = "Clipshot"
    const val AUTHORITY = "dev.sidali.clipshot.shots"
    private const val KEEP = 3          // last copies kept, so the clipboard URI stays readable
    private const val WINDOW_S = 30L    // only images added in the last 30 s are considered

    /** READ_MEDIA_IMAGES exists from Android 13; before that, READ_EXTERNAL_STORAGE. */
    val READ_PERMISSION: String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasAccess(ctx: Context): Boolean = ctx.checkSelfPermission(READ_PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** "All files access", only needed for the optional "remove from gallery" mode. */
    fun canDelete(): Boolean = BuildConfig.REMOVE_FROM_GALLERY && Environment.isExternalStorageManager()

    fun dir(ctx: Context) = File(ctx.noBackupFilesDir, "shots").apply { mkdirs() }

    /** Returns true when a new screenshot was copied. */
    fun copyLatest(ctx: Context): Boolean {
        if (!Prefs.enabled(ctx) || !hasAccess(ctx)) return false
        val now = System.currentTimeMillis() / 1000
        val col = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.OWNER_PACKAGE_NAME, MediaStore.Images.Media.DATE_ADDED,
        )
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.DATE_ADDED} >= ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("${now - WINDOW_S}"))
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, 5)
        }
        var id = -1L; var mime: String? = null; var addedAt = 0L
        ctx.contentResolver.query(col, proj, args, null)?.use { c ->
            while (c.moveToNext()) {
                if (Rules.isScreenshot(c.getString(2), c.getString(1), c.getString(4))) { id = c.getLong(0); mime = c.getString(3); addedAt = c.getLong(5); break }
            }
        }
        if (id < 0 || id == Prefs.lastId(ctx)) return false

        val name = "shot-$id.${Rules.extensionFor(mime)}"
        val out = File(dir(ctx), name)
        val tmp = File(dir(ctx), "$name.part")
        try {
            ctx.contentResolver.openInputStream(ContentUris.withAppendedId(col, id))
                ?.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } } ?: return false
            if (tmp.length() == 0L || !tmp.renameTo(out)) { tmp.delete(); return false }
        } catch (t: Throwable) {
            Log.w(TAG, "read failed, the screenshot may still be being written", t); tmp.delete(); return false
        }

        val uri = Uri.parse("content://$AUTHORITY/$name")
        ctx.getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newUri(ctx.contentResolver, ctx.getString(R.string.clip_label), uri))
        Prefs.recordCopy(ctx, id, name)
        prune(ctx)
        if (Prefs.removeFromGallery(ctx) && canDelete()) Cleanup.later(ctx, id, addedAt)
        return true
    }

    /** True while a screenshot from the last few seconds is still being written (MediaStore "pending"). */
    fun screenshotPending(ctx: Context): Boolean {
        val now = System.currentTimeMillis() / 1000
        val args = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_ONLY)
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.DATE_ADDED} >= ?")
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("${now - WINDOW_S}"))
        }
        val proj = arrayOf(
            MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.OWNER_PACKAGE_NAME,
        )
        ctx.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, args, null)?.use { c ->
            while (c.moveToNext()) if (Rules.isScreenshot(c.getString(0), c.getString(1), c.getString(2))) return true
        }
        return false
    }

    private fun prune(ctx: Context) {
        val files = dir(ctx).listFiles() ?: return
        files.filter { it.name.endsWith(".part") && it.lastModified() < System.currentTimeMillis() - 60_000 }.forEach { it.delete() }
        files.filter { Rules.isSafeName(it.name) }.sortedByDescending { it.lastModified() }.drop(KEEP).forEach { it.delete() }
    }

    fun lastFile(ctx: Context): File? =
        Prefs.lastName(ctx)?.takeIf { Rules.isSafeName(it) }?.let { File(dir(ctx), it) }?.takeIf { it.exists() }
}
