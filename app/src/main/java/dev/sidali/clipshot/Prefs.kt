package dev.sidali.clipshot

import android.content.Context

object Prefs {
    private fun p(ctx: Context) = ctx.getSharedPreferences("clipshot", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = p(ctx).getBoolean("enabled", true)
    fun setEnabled(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("enabled", v).apply()
    fun lastId(ctx: Context) = p(ctx).getLong("lastId", -1)
    fun lastName(ctx: Context): String? = p(ctx).getString("lastName", null)
    fun lastTime(ctx: Context) = p(ctx).getLong("lastTime", 0)
    fun count(ctx: Context) = p(ctx).getInt("count", 0)
    fun recordCopy(ctx: Context, id: Long, name: String) {
        val s = p(ctx)
        s.edit().putLong("lastId", id).putString("lastName", name)
            .putLong("lastTime", System.currentTimeMillis()).putInt("count", s.getInt("count", 0) + 1).commit()
    }
}
