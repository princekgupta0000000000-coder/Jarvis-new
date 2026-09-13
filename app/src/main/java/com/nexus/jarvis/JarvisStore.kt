package com.nexus.jarvis

import android.content.Context
import org.json.JSONArray

/** Small offline-first store. Keeps user tasks/settings after app restarts without a server. */
class JarvisStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_store", Context.MODE_PRIVATE)

    fun getName(): String = prefs.getString("name", "Prince") ?: "Prince"
    fun setName(value: String) { prefs.edit().putString("name", value).apply() }

    fun getTasks(): List<String> {
        val raw = prefs.getString("tasks", null) ?: return listOf("Physics revision", "Biology MCQs", "College assignment")
        return runCatching {
            val a = JSONArray(raw)
            List(a.length()) { i -> a.getString(i) }
        }.getOrDefault(emptyList())
    }

    fun setTasks(tasks: List<String>) {
        val a = JSONArray()
        tasks.forEach(a::put)
        prefs.edit().putString("tasks", a.toString()).apply()
    }

    fun getBool(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun setBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
}
