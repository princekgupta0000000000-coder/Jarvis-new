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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Offline-first command/knowledge layer. Safe Android actions are deterministic; unknown questions get useful local answers. */
object JarvisCommandRouter {
    private val fullForms = mapOf(
        "iai" to "Introduction to Artificial Intelligence",
        "cfet" to "Computer Fundamentals & Emerging Technologies",
        "be" to "Basic Electrical & Electronics Engineering",
        "phy" to "Engineering Physics",
        "em-i" to "Engineering Mathematics-I",
        "eic" to "Essence of Indian Constitution",
        "uhv" to "Universal Human Values",
        "pps lab" to "Programming for Problem Solving Lab",
        "be lab" to "Basic Electrical & Electronics Engineering Lab",
        "phy lab" to "Engineering Physics Lab"
    )

    private val holidayDates = setOf(
        "2026-08-02","2026-08-04","2026-08-09","2026-08-16","2026-08-23","2026-08-26","2026-08-28","2026-08-30",
        "2026-09-04","2026-09-06","2026-09-13","2026-09-20","2026-09-27",
        "2026-10-02","2026-10-04","2026-10-11","2026-10-17","2026-10-18","2026-10-19","2026-10-20","2026-10-25",
        "2026-11-01","2026-11-08","2026-11-09","2026-11-10","2026-11-11","2026-11-12","2026-11-13","2026-11-14","2026-11-15","2026-11-16","2026-11-22","2026-11-24","2026-11-29",
        "2026-12-06","2026-12-13","2026-12-20","2026-12-25","2026-12-26","2026-12-27","2026-12-28","2026-12-29","2026-12-30","2026-12-31"
    )

    fun execute(context: Context, raw: String): String {
        val command = raw.trim()
        val lower = normalize(command)
        if (lower.isBlank()) return "I didn't catch that."

        if (lower.matches(Regex("(hello|hi|hey)( jarvis)?"))) return "Hello Prince. JARVIS is online and ready."
        if (containsAny(lower, "who are you", "what can you do", "tum kaun ho", "aap kaun ho")) return "I am JARVIS, your local Android assistant. I can answer common questions, understand your timetable, manage study tasks and attendance, search the web, and safely open phone apps."

        // Direct app actions. Package visibility is declared in AndroidManifest.
        val apps = listOf(
            "youtube" to Pair("com.google.android.youtube", "https://www.youtube.com"),
            "chrome" to Pair("com.android.chrome", "https://www.google.com"),
            "whatsapp" to Pair("com.whatsapp", "https://web.whatsapp.com"),
            "maps" to Pair("com.google.android.apps.maps", "https://maps.google.com"),
            "google maps" to Pair("com.google.android.apps.maps", "https://maps.google.com"),
            "gmail" to Pair("com.google.android.gm", "https://mail.google.com"),
            "calendar" to Pair("com.google.android.calendar", "https://calendar.google.com"),
            "messages" to Pair("com.google.android.apps.messaging", "https://messages.google.com/web")
        )
        val requestedApp = apps.firstOrNull { (name, _) ->
            lower == name || lower.contains("$name kholo") || lower.contains("$name khol") || lower.contains("open $name") || lower.contains("launch $name") || lower.contains("start $name")
        }
        if (requestedApp != null) {
            val (name, target) = requestedApp
            if (name == "youtube") {
                val query = extractAfter(lower, listOf("youtube par ", "youtube pe ", "youtube me ", "youtube "))
                if (query.isNotBlank() && !isGenericWord(query)) {
                    openUrl(context, "https://www.youtube.com/results?search_query=${encode(query)}")
                    return "Opening YouTube for $query"
                }
            }
            openAppOrWeb(context, target.first, target.second)
            return "Opening ${pretty(name)}"
        }
        if (containsAny(lower, "settings kholo", "settings khol", "open settings")) {
            safeLaunch(context, Intent(Settings.ACTION_SETTINGS)); return "Opening Settings"
        }
        if (containsAny(lower, "camera kholo", "camera khol", "open camera")) {
            safeLaunch(context, Intent("android.media.action.IMAGE_CAPTURE")); return "Opening Camera"
        }
        if (containsAny(lower, "phone kholo", "dialer kholo", "dialer khol", "open phone")) {
            safeLaunch(context, Intent(Intent.ACTION_DIAL)); return "Opening Phone"
        }

        if (lower.startsWith("play ") && containsAny(lower, "on youtube", "youtube par", "youtube pe")) {
            val query = lower.removePrefix("play ").replace(" on youtube", "").replace(" youtube par", "").replace(" youtube pe", "").trim()
            openUrl(context, "https://www.youtube.com/results?search_query=${encode(query)}")
            return "Searching YouTube for $query"
        }

        if (containsAny(lower, "google kholo", "google khol", "open google", "open browser", "browser kholo")) {
            openUrl(context, "https://www.google.com"); return "Opening Google"
        }
        val searchPrefix = listOf("search for ", "search ", "google search ", "google par ", "google pe ", "find ")
            .firstOrNull { lower.startsWith(it) }
        if (searchPrefix != null) {
            val query = lower.removePrefix(searchPrefix).trim()
            if (query.isNotBlank()) { openUrl(context, "https://www.google.com/search?q=${encode(query)}"); return "Searching Google for $query" }
        }

        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.startsWith("number ")) {
            val number = command.substringAfter(' ').filter { it.isDigit() || it == '+' }
            return if (number.isBlank()) "Please provide a phone number." else { safeLaunch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))); "Opening the dialer for $number" }
        }
        if (lower.startsWith("text ") || lower.startsWith("sms ") || lower.startsWith("message ")) {
            val message = command.substringAfter(' ', "").trim()
            if (message.isBlank()) return "Tell me the message."
            safeLaunch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply { putExtra("sms_body", message) })
            return "Opening Messages for your confirmation."
        }
        if (containsAny(lower, "add event", "calendar event", "schedule meeting")) {
            safeLaunch(context, Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)); return "Opening Calendar to add an event."
        }
        if (containsAny(lower, "alarm kholo", "alarm khol", "open alarm", "remind me", "reminder")) {
            safeLaunch(context, Intent(AlarmClock.ACTION_SET_ALARM)); return "Opening the alarm and reminder screen."
        }

        // Timetable intelligence.
        if (containsAny(lower, "today class", "aaj ki class", "aaj class", "class kya", "class kab", "next class", "agli class", "timetable", "schedule")) {
            return todaySchedule()
        }
        if (containsAny(lower, "tomorrow class", "kal ki class", "kal class")) return scheduleFor(LocalDate.now().plusDays(1))
        if (containsAny(lower, "monday class", "monday ka schedule")) return scheduleFor(LocalDate.of(2026, 8, 17).with(DayOfWeek.MONDAY))
        if (containsAny(lower, "tuesday class", "tuesday ka schedule")) return "Tuesday: 11:40–12:30 PHY, 12:30–1:20 BE, 2:20–3:10 EIC. Baaki periods free. Room 204."
        if (containsAny(lower, "wednesday class", "wednesday ka schedule")) return "Wednesday: 10:00–10:50 PHY, 10:50–11:40 EM-I, 11:40–12:30 BE, 12:30–1:20 IAI, 2:20–3:10 EIC, 4:00–5:00 UHV. Room 204."
        if (containsAny(lower, "thursday class", "thursday ka schedule")) return "Thursday (G2): No G1 labs. 2:20–3:10 Free, 3:10–4:00 EM-I, 4:00–5:00 UHV. Room 204."
        if (containsAny(lower, "friday class", "friday ka schedule")) return "Friday (G2): 10:00–11:40 PPS LAB, 11:40–1:20 BE LAB, 2:20–3:10 PHY, 3:10–4:00 EM-I. Room 204."
        if (containsAny(lower, "saturday class", "saturday ka schedule")) return "Saturday: 10:00–10:50 CFET, 10:50–11:40 CFET, 11:40–12:30 IAI. After that free. Room 204."

        if (lower.contains("full form") || lower.contains("meaning of") || lower.contains("what is iai") || lower.contains("what is cfet") || lower.contains("what is be") || lower.contains("what is phy") || lower.contains("what is em-i") || lower.contains("what is eic") || lower.contains("what is uhv")) {
            val hit = fullForms.entries.firstOrNull { lower.contains(it.key) }
            if (hit != null) return "${hit.key.uppercase(Locale.US)} means ${hit.value}."
        }
        if (containsAny(lower, "lab attendance", "lab kitni attendance", "lab attendance count")) return "G2 labs: PHY LAB Monday, PPS LAB Friday, BE LAB Friday. Each complete lab session counts as ONE attendance, even though it occupies two periods."
        if (containsAny(lower, "holiday", "chhutti", "holiday kab", "no class")) return holidayAnswer(LocalDate.now())

        if (containsAny(lower, "attendance", "meri attendance", "attendance kitni", "safe leave", "kitni class chhod")) return "Attendance calculator is available in CORE. Enter present, total and target %. JARVIS will calculate the safe number of classes you can miss. Labs are counted as one attendance per lab session."
        if (containsAny(lower, "tasks", "task batao", "study plan", "padhai", "what should i study")) return "Your persistent tasks are in TASKS. For a study session, start with the nearest pending task, then revise one weak topic and finish with MCQs."

        // Small offline conversational knowledge layer.
        if (containsAny(lower, "what is ai", "ai kya hai", "artificial intelligence kya hai")) return "AI means Artificial Intelligence: computers performing tasks that normally need human intelligence, such as understanding language, recognizing patterns and making predictions."
        if (containsAny(lower, "what is machine learning", "machine learning kya hai", "ml kya hai")) return "Machine Learning is a part of AI where a model learns patterns from data and uses them to make predictions or decisions."
        if (containsAny(lower, "what is kotlin", "kotlin kya hai")) return "Kotlin is a modern programming language widely used for Android development."
        if (containsAny(lower, "what is android", "android kya hai")) return "Android is Google's mobile operating system and application platform."
        if (containsAny(lower, "time kya hai", "what time is it", "current time")) return "Current device time: ${LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"))}."
        if (containsAny(lower, "date kya hai", "what date is it", "today date")) return "Today is ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}."

        return "I heard: $command. I can handle phone actions, timetable, attendance, study tasks and common questions. For a completely open-ended AI answer, the selected GGUF model still needs a local inference engine; I won't pretend that layer is already connected."
    }

    private fun todaySchedule(): String = scheduleFor(LocalDate.now())

    private fun scheduleFor(date: LocalDate): String {
        if (date.isBefore(LocalDate.of(2026,8,18)) || date.isAfter(LocalDate.of(2026,12,31))) return "Your semester timetable is effective from 18 August 2026 to 31 December 2026."
        if (holidayDates.contains(date.toString()) || date.dayOfWeek == DayOfWeek.SUNDAY) return "No class today (${date.format(DateTimeFormatter.ofPattern("dd MMM"))}). It is marked as a holiday/no-class day."
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "Today: 10:00 IAI, 10:50 CFET, 11:40 BE, 12:30 EIC, 2:20–4:00 PHY LAB (G2). Room 204."
            DayOfWeek.TUESDAY -> "Today: 11:40–12:30 PHY, 12:30–1:20 BE, 2:20–3:10 EIC. Other periods are free. Room 204."
            DayOfWeek.WEDNESDAY -> "Today: 10:00 PHY, 10:50 EM-I, 11:40 BE, 12:30 IAI, 2:20 EIC, 4:00 UHV. Room 204."
            DayOfWeek.THURSDAY -> "Today (G2): No G1 labs. 2:20–3:10 Free, 3:10–4:00 EM-I, 4:00–5:00 UHV. Room 204."
            DayOfWeek.FRIDAY -> "Today (G2): 10:00–11:40 PPS LAB, 11:40–1:20 BE LAB, 2:20 PHY, 3:10 EM-I. Room 204."
            DayOfWeek.SATURDAY -> "Today: 10:00 CFET, 10:50 CFET, 11:40 IAI. 12:30 onward free. Room 204."
            else -> "No class today."
        }
    }

    private fun holidayAnswer(date: LocalDate): String = if (holidayDates.contains(date.toString()) || date.dayOfWeek == DayOfWeek.SUNDAY) "Today is a no-class/holiday day." else "Today is not in the stored holiday list; follow the college's latest notice for any special cancellation."

    private fun normalize(value: String): String = value.lowercase(Locale.US).replace(Regex("\\s+"), " ").trim()
    private fun containsAny(text: String, vararg values: String): Boolean = values.any(text::contains)
    private fun extractAfter(text: String, prefixes: List<String>): String { val p = prefixes.firstOrNull { text.contains(it) } ?: return ""; return text.substringAfter(p).trim() }
    private fun isGenericWord(value: String): Boolean = value in setOf("khol", "kholo", "open", "chala", "chalao")
    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    private fun pretty(value: String): String = value.replaceFirstChar { it.uppercase(Locale.US) }

    private fun openAppOrWeb(context: Context, packageName: String, fallbackUrl: String) {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) safeLaunch(context, launch) else openUrl(context, fallbackUrl)
    }
    private fun openUrl(context: Context, url: String) { safeLaunch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addCategory(Intent.CATEGORY_BROWSABLE) }) }
    private fun safeLaunch(context: Context, intent: Intent): Boolean {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { context.startActivity(intent); true } catch (_: ActivityNotFoundException) { false }
    }
}