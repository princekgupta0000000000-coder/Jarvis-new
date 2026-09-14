package com.nexus.jarvis

import android.content.Context
import org.json.JSONArray

/** Offline-first persistence for JARVIS user data. */
class JarvisStore(context: Context) {
    private val prefs = context.getSharedPreferences("jarvis_store", Context.MODE_PRIVATE)

    fun getName(): String = prefs.getString("name", "Prince") ?: "Prince"
    fun setName(value: String) { prefs.edit().putString("name", value).apply() }

    fun getTasks(): List<String> = getList("tasks", listOf("Physics revision", "Biology MCQs", "College assignment"))
    fun setTasks(tasks: List<String>) = setList("tasks", tasks)

    fun getSchedule(): List<String> = getList("schedule", listOf(
        "MONDAY|Maths|09:00|Room 101",
        "MONDAY|Physics|10:00|Room 102",
        "TUESDAY|Biology|09:00|Room 201",
        "TUESDAY|Chemistry|10:00|Room 203",
        "WEDNESDAY|Physics|09:00|Room 102",
        "WEDNESDAY|Maths|11:00|Room 101",
        "THURSDAY|Computer Fundamentals|09:00|Lab 1",
        "FRIDAY|Basic Electronics|10:00|Room 204"
    ))
    fun setSchedule(rows: List<String>) = setList("schedule", rows)

    fun getPresent(): Int = prefs.getInt("present", 0)
    fun getTotal(): Int = prefs.getInt("total", 0)
    fun getTarget(): Int = prefs.getInt("target", 75)
    fun setAttendance(present: Int, total: Int, target: Int) {
        prefs.edit().putInt("present", present).putInt("total", total).putInt("target", target).apply()
    }

    fun getBool(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun setBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }

    private fun getList(key: String, fallback: List<String>): List<String> {
        val raw = prefs.getString(key, null) ?: return fallback
        return runCatching {
            val a = JSONArray(raw)
            List(a.length()) { i -> a.getString(i) }
        }.getOrDefault(fallback)
    }

    private fun setList(key: String, values: List<String>) {
        val a = JSONArray()
        values.forEach(a::put)
        prefs.edit().putString(key, a.toString()).apply()
    }
}
