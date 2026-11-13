package com.hucw.skipads

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SkipLogStore {

    private const val FILE = "skipads_logs"
    private const val KEY_LOG = "log"
    private const val MAX_ENTRIES = 30

    data class Entry(val time: String, val pkg: String, val text: String)

    private fun sp(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun timeFormat() = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)

    fun add(context: Context, pkg: String, text: String) {
        val entry = JSONObject()
            .put("t", timeFormat().format(Date()))
            .put("p", pkg)
            .put("x", text)
        val array = readArray(context)
        array.put(entry)
        while (array.length() > MAX_ENTRIES) {
            array.remove(0)
        }
        sp(context).edit().putString(KEY_LOG, array.toString()).apply()
    }

    fun recent(context: Context, limit: Int): List<Entry> {
        val array = readArray(context)
        val result = ArrayList<Entry>()
        var index = array.length() - 1
        while (index >= 0 && result.size < limit) {
            val item = array.optJSONObject(index)
            if (item != null) {
                result.add(
                    Entry(
                        time = item.optString("t"),
                        pkg = item.optString("p"),
                        text = item.optString("x")
                    )
                )
            }
            index--
        }
        return result
    }

    fun clear(context: Context) {
        sp(context).edit().remove(KEY_LOG).apply()
    }

    private fun readArray(context: Context): JSONArray {
        val raw = sp(context).getString(KEY_LOG, null) ?: return JSONArray()
        return runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
    }
}
