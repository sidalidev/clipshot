package dev.sidali.clipshot

import android.content.Context

object Prefs {
    private fun p(ctx: Context) = ctx.getSharedPreferences("clipshot", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = p(ctx).getBoolean("enabled", true)
    fun setEnabled(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("enabled", v).apply() }
    fun removeFromGallery(ctx: Context) = p(ctx).getBoolean("removeFromGallery", false)
    fun setRemoveFromGallery(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("removeFromGallery", v).apply() }
    fun lastId(ctx: Context) = p(ctx).getLong("lastId", -1)
    fun lastName(ctx: Context): String? = p(ctx).getString("lastName", null)
    fun lastTime(ctx: Context) = p(ctx).getLong("lastTime", 0)
    fun count(ctx: Context) = p(ctx).getInt("count", 0)

    fun recordCopy(ctx: Context, id: Long, name: String) {
        val s = p(ctx)
        s.edit().putLong("lastId", id).putString("lastName", name)
            .putLong("lastTime", System.currentTimeMillis()).putInt("count", s.getInt("count", 0) + 1).commit()
    }

    /** Pending gallery removals, stored as "id:dueMillis:dateAddedSeconds". */
    @Synchronized fun pendingDeletes(ctx: Context): Map<Long, Pair<Long, Long>> =
        p(ctx).getStringSet("pendingDeletes", emptySet())!!.mapNotNull { e ->
            val f = e.split(':').map { it.toLongOrNull() }
            if (f.size == 3 && f.all { it != null }) f[0]!! to (f[1]!! to f[2]!!) else null
        }.toMap()

    private fun save(ctx: Context, m: Map<Long, Pair<Long, Long>>) {
        p(ctx).edit().putStringSet("pendingDeletes", m.map { "${it.key}:${it.value.first}:${it.value.second}" }.toSet()).commit()
    }

    @Synchronized fun addPendingDelete(ctx: Context, id: Long, due: Long, addedAt: Long) =
        save(ctx, pendingDeletes(ctx) + (id to (due to addedAt)))

    @Synchronized fun removePendingDelete(ctx: Context, id: Long) = save(ctx, pendingDeletes(ctx) - id)
}
