package com.nexus.jarvis

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Deterministic local command layer. Voice text is normalized first, then mapped to
 * safe Android intents. This runs before/alongside the local LLM so basic device
 * commands do not depend on model output.
 */
object JarvisCommandRouter {
    fun execute(context: Context, raw: String): String {
        val command = raw.trim()
        val lower = normalize(command)
        if (lower.isBlank()) return "I didn't catch that."

        // Greetings / identity
        if (lower.matches(Regex("(hello|hi|hey)( jarvis)?"))) return "Hello Prince. JARVIS is ready."
        if (containsAny(lower, "who are you", "what can you do", "tum kaun ho", "aap kaun ho")) {
            return "I am JARVIS. I can open apps and websites, manage your schedule, study tasks, attendance, reminders and safe phone actions."
        }

        // YouTube: English + common Hindi/Hinglish voice variants.
        if (containsAny(lower, "youtube kholo", "youtube khol", "youtube open", "open youtube", "youtube chala", "youtube chalao")) {
            val query = extractAfter(lower, listOf("youtube par ", "youtube pe ", "youtube me ", "youtube "))
            return if (query.isNotBlank() && !isGenericWord(query)) {
                openUrl(context, "https://www.youtube.com/results?search_query=${encode(query)}")
                "Opening YouTube for $query"
            } else {
                openAppOrWeb(context, "com.google.android.youtube", "https://www.youtube.com")
                "Opening YouTube"
            }
        }
        if (lower.startsWith("play ") && containsAny(lower, "on youtube", "youtube par", "youtube pe")) {
            val query = lower.removePrefix("play ").replace(" on youtube", "").replace(" youtube par", "").replace(" youtube pe", "").trim()
            openUrl(context, "https://www.youtube.com/results?search_query=${encode(query)}")
            return "Searching YouTube for $query"
        }

        // Common apps. Use launcher intent first, then a web fallback.
        if (containsAny(lower, "chrome kholo", "chrome khol", "open chrome")) {
            openAppOrWeb(context, "com.android.chrome", "https://www.google.com")
            return "Opening Chrome"
        }
        if (containsAny(lower, "whatsapp kholo", "whatsapp khol", "open whatsapp")) {
            openAppOrWeb(context, "com.whatsapp", "https://web.whatsapp.com")
            return "Opening WhatsApp"
        }
        if (containsAny(lower, "settings kholo", "settings khol", "open settings")) {
            safeLaunch(context, Intent(Settings.ACTION_SETTINGS))
            return "Opening Settings"
        }

        // Generic web/search commands.
        if (containsAny(lower, "google kholo", "google khol", "open google", "open browser", "browser kholo")) {
            openUrl(context, "https://www.google.com")
            return "Opening Google"
        }
        val searchPrefix = listOf("search for ", "search ", "google search ", "google par ", "google pe ")
            .firstOrNull { lower.startsWith(it) }
        if (searchPrefix != null) {
            val query = lower.removePrefix(searchPrefix).trim()
            if (query.isNotBlank()) {
                openUrl(context, "https://www.google.com/search?q=${encode(query)}")
                return "Searching Google for $query"
            }
        }

        // Calls / dialer. We deliberately open the dialer rather than silently placing a call.
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("number ")) {
            val number = command.substringAfter(' ').filter { it.isDigit() || it == '+' }
            return if (number.isBlank()) "Please provide a phone number." else {
                safeLaunch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
                "Opening the dialer for $number"
            }
        }

        // SMS compose, user confirms by pressing send.
        if (lower.startsWith("text ") || lower.startsWith("sms ") || lower.startsWith("message ")) {
            val message = command.substringAfter(' ', "").trim()
            if (message.isBlank()) return "Tell me the message."
            safeLaunch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                putExtra("sms_body", message)
            })
            return "Opening Messages for your confirmation."
        }

        // Calendar and alarms.
        if (containsAny(lower, "calendar kholo", "calendar khol", "open calendar", "add event", "calendar")) {
            safeLaunch(context, Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI))
            return "Opening Calendar to add an event."
        }
        if (containsAny(lower, "alarm kholo", "alarm khol", "open alarm", "remind me", "reminder")) {
            safeLaunch(context, Intent(AlarmClock.ACTION_SET_ALARM))
            return "Opening the alarm and reminder screen."
        }

        // Schedule / attendance / study responses are deterministic until user data is configured.
        if (containsAny(lower, "schedule", "timetable", "time table", "class kab", "class kya", "aaj ki class", "today class")) {
            return "Your current schedule is available in the Schedule tab. Configure your real timetable there; I won't invent a class time."
        }
        if (containsAny(lower, "attendance", "meri attendance", "attendance kitni")) {
            return "Open the Attendance section to see your saved percentage and safe-leave calculation."
        }
        if (containsAny(lower, "tasks", "task batao", "study plan", "padhai", "what should i study")) {
            return "Your study tasks are available in the Tasks and Study tabs."
        }

        return "I heard: $command. I don't have a safe action mapped for that yet."
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("\\s+"), " ").trim()

    private fun containsAny(text: String, vararg values: String): Boolean = values.any(text::contains)

    private fun extractAfter(text: String, prefixes: List<String>): String {
        val prefix = prefixes.firstOrNull { text.contains(it) } ?: return ""
        return text.substringAfter(prefix).trim()
    }

    private fun isGenericWord(value: String): Boolean = value in setOf("khol", "kholo", "open", "chala", "chalao")

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

    private fun openAppOrWeb(context: Context, packageName: String, fallbackUrl: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            safeLaunch(context, launch)
        } else {
            openUrl(context, fallbackUrl)
        }
    }

    private fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        safeLaunch(context, intent)
    }

    private fun safeLaunch(context: Context, intent: Intent): Boolean {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
