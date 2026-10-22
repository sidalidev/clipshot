package dev.sidali.clipshot

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import java.io.File

/** Finds the screenshot the system just saved and puts a copy of it on the clipboard. */
object Shots {
    private const val TAG = "Clipshot"
    const val AUTHORITY = "dev.sidali.clipshot.shots"
    private const val KEEP = 3          // last copies kept so the clipboard URI stays readable
    private const val WINDOW_S = 30L    // only screenshots added in the last 30 s count

    fun hasAccess(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED

    fun dir(ctx: Context) = File(ctx.filesDir, "shots").apply { mkdirs() }

    /** Returns true when a new screenshot was copied. */
    fun copyLatest(ctx: Context): Boolean {
        if (!Prefs.enabled(ctx) || !hasAccess(ctx)) return false
        val now = System.currentTimeMillis() / 1000
        val col = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val proj = arrayOf(
            MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH, MediaStore.Images.Media.MIME_TYPE,
        )
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Images.Media.DATE_ADDED} >= ? AND (${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? " +
                    "OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?)",
            )
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("${now - WINDOW_S}", "%Screenshot%", "Screenshot%"))
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        }
        var id = -1L; var mime = "image/png"
        ctx.contentResolver.query(col, proj, args, null)?.use { c ->
            if (c.moveToFirst()) { id = c.getLong(0); mime = c.getString(3) ?: mime }
        }
        if (id < 0 || id == Prefs.lastId(ctx)) return false

        val ext = when (mime) { "image/jpeg" -> "jpg"; "image/webp" -> "webp"; else -> "png" }
        val name = "shot-$id.$ext"
        val out = File(dir(ctx), name)
        val src = ContentUris.withAppendedId(col, id)
        try {
            ctx.contentResolver.openInputStream(src)?.use { i -> out.outputStream().use { o -> i.copyTo(o) } } ?: return false
        } catch (t: Throwable) {
            Log.w(TAG, "read failed (screenshot still being written?)", t); out.delete(); return false
        }
        if (out.length() == 0L) { out.delete(); return false }

        val uri = Uri.parse("content://$AUTHORITY/$name")
        val cm = ctx.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newUri(ctx.contentResolver, ctx.getString(R.string.clip_label), uri))
        Prefs.recordCopy(ctx, id, name)
        prune(ctx)
        Log.i(TAG, "copied $name")
        return true
    }

    private fun prune(ctx: Context) {
        dir(ctx).listFiles()?.sortedByDescending { it.lastModified() }?.drop(KEEP)?.forEach { it.delete() }
    }

    fun lastFile(ctx: Context): File? = Prefs.lastName(ctx)?.let { File(dir(ctx), it) }?.takeIf { it.exists() }
}
