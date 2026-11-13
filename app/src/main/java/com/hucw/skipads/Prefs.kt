package com.hucw.skipads

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prefs {

    private const val FILE = "skipads_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_WINDOW_MS = "window_ms"
    private const val KEY_KEYWORDS = "keywords"
    private const val KEY_TOTAL = "total_skips"
    private const val KEY_DAY = "day"
    private const val KEY_TODAY = "today_skips"

    const val DEFAULT_WINDOW_MS = 6000L

    val DEFAULT_KEYWORDS: List<String> = listOf(
        "跳过",
        "跳过广告",
        "跳过开屏",
        "跳过此广告",
        "关闭广告",
        "skip",
        "skip ad",
        "skip ads"
    )

    private fun sp(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean =
        sp(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, value: Boolean) {
        sp(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun windowMs(context: Context): Long =
        sp(context).getLong(KEY_WINDOW_MS, DEFAULT_WINDOW_MS)

    fun setWindowMs(context: Context, value: Long) {
        sp(context).edit().putLong(KEY_WINDOW_MS, value).apply()
    }

    fun keywords(context: Context): List<String> =
        sp(context).getString(KEY_KEYWORDS, null)
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_KEYWORDS

    fun setKeywords(context: Context, keywords: List<String>) {
        sp(context).edit().putString(KEY_KEYWORDS, keywords.joinToString("\n")).apply()
    }

    fun recordSkip(context: Context) {
        val today = todayKey()
        val prefs = sp(context)
        val todayCount = if (prefs.getString(KEY_DAY, null) == today) {
            prefs.getInt(KEY_TODAY, 0) + 1
        } else {
            1
        }
        prefs.edit()
            .putString(KEY_DAY, today)
            .putInt(KEY_TODAY, todayCount)
            .putInt(KEY_TOTAL, prefs.getInt(KEY_TOTAL, 0) + 1)
            .apply()
    }

    fun stats(context: Context): Pair<Int, Int> {
        val prefs = sp(context)
        val todayCount = if (prefs.getString(KEY_DAY, null) == todayKey()) {
            prefs.getInt(KEY_TODAY, 0)
        } else {
            0
        }
        return todayCount to prefs.getInt(KEY_TOTAL, 0)
    }

    private fun todayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
