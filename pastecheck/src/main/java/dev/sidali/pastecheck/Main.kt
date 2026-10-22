package dev.sidali.pastecheck
import android.app.Activity
import android.content.ClipboardManager
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.TextView
class Main : Activity() {
    override fun onWindowFocusChanged(has: Boolean) {
        super.onWindowFocusChanged(has); if (!has) return
        val msg = try {
            val clip = getSystemService(ClipboardManager::class.java).primaryClip
            val uri = clip?.getItemAt(0)?.uri
            val mime = clip?.description?.getMimeType(0)
            val bmp = uri?.let { contentResolver.openInputStream(it)?.use { s -> BitmapFactory.decodeStream(s) } }
            "PASTE_OK uri=$uri mime=$mime size=${bmp?.width}x${bmp?.height}"
        } catch (t: Throwable) { "PASTE_FAIL $t" }
        Log.i("PasteCheck", msg); setContentView(TextView(this).apply { text = msg; textSize = 18f; setPadding(40, 200, 40, 40) })
    }
}
