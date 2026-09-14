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

    /** SIT Sitamarhi CSE (AI & ML), Semester 1, 2026-27. User is G2. */
    fun getSchedule(): List<String> = getList("schedule", listOf(
        "MONDAY|IAI|10:00-10:50|Room 204",
        "MONDAY|CFET|10:50-11:40|Room 204",
        "MONDAY|BE|11:40-12:30|Room 204",
        "MONDAY|EIC|12:30-13:20|Room 204",
        "MONDAY|PHY LAB (G2)|14:20-16:00|Room 204",
        "TUESDAY|FREE|10:00-11:40|Room 204",
        "TUESDAY|PHY|11:40-12:30|Room 204",
        "TUESDAY|BE|12:30-13:20|Room 204",
        "TUESDAY|EIC|14:20-15:10|Room 204",
        "TUESDAY|FREE|15:10-17:00|Room 204",
        "WEDNESDAY|PHY|10:00-10:50|Room 204",
        "WEDNESDAY|EM-I|10:50-11:40|Room 204",
        "WEDNESDAY|BE|11:40-12:30|Room 204",
        "WEDNESDAY|IAI|12:30-13:20|Room 204",
        "WEDNESDAY|EIC|14:20-15:10|Room 204",
        "WEDNESDAY|FREE|15:10-16:00|Room 204",
        "WEDNESDAY|UHV|16:00-17:00|Room 204",
        "THURSDAY|NO CLASS (G2)|10:00-13:20|Room 204",
        "THURSDAY|FREE|14:20-15:10|Room 204",
        "THURSDAY|EM-I|15:10-16:00|Room 204",
        "THURSDAY|UHV|16:00-17:00|Room 204",
        "FRIDAY|PPS LAB (G2)|10:00-11:40|Room 204",
        "FRIDAY|BE LAB (G2)|11:40-13:20|Room 204",
        "FRIDAY|PHY|14:20-15:10|Room 204",
        "FRIDAY|EM-I|15:10-16:00|Room 204",
        "FRIDAY|FREE|16:00-17:00|Room 204",
        "SATURDAY|CFET|10:00-10:50|Room 204",
        "SATURDAY|CFET|10:50-11:40|Room 204",
        "SATURDAY|IAI|11:40-12:30|Room 204",
        "SATURDAY|FREE|12:30-13:20|Room 204",
        "SATURDAY|FREE|14:20-17:00|Room 204"
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