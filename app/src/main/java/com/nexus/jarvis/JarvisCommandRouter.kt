package com.nexus.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings

/** Local-first command router. Sensitive actions open the system UI for user confirmation. */
object JarvisCommandRouter {
    fun execute(context: MainActivity, raw: String): String {
        val command = raw.trim()
        val lower = command.lowercase()
        return when {
            lower.contains("open youtube") -> openAppOrWeb(context, "com.google.android.youtube", "https://youtube.com", "Opening YouTube")
            lower.contains("open chrome") -> openAppOrWeb(context, "com.android.chrome", "https://google.com", "Opening Chrome")
            lower.contains("open whatsapp") -> openAppOrWeb(context, "com.whatsapp", "https://web.whatsapp.com", "Opening WhatsApp")
            lower.contains("open google") || lower.contains("open browser") -> { context.openBrowser("google.com"); "Opening Google" }
            lower.startsWith("call ") || lower.startsWith("dial ") -> {
                val number = command.substringAfter(' ').filter { it.isDigit() || it == '+' }
                if (number.isBlank()) "Please provide a phone number" else { context.dial(number); "Opening the dialer for $number" }
            }
            lower.startsWith("text ") || lower.startsWith("sms ") -> {
                val message = command.substringAfter(' ', "").trim()
                if (message.isBlank()) "Tell me the message" else {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
                    intent.putExtra("sms_body", message)
                    context.startActivity(intent)
                    "Opening messages for your confirmation"
                }
            }
            lower.contains("calendar") || lower.contains("add event") -> {
                val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                context.startActivity(intent)
                "Opening Calendar to add the event"
            }
            lower.contains("alarm") || lower.contains("remind me") -> {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, command)
                }
                if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                "Opening the alarm/reminder screen"
            }
            lower.contains("settings") -> { context.startActivity(Intent(Settings.ACTION_SETTINGS)); "Opening Settings" }
            lower.contains("attendance") -> "Your attendance dashboard is available in JARVIS. Use the Attendance card to calculate safe leaves."
            lower.contains("who are you") || lower.contains("what can you do") -> "I am JARVIS. I can manage your schedule, study tasks, attendance, reminders, browser, apps and safe device actions."
            lower.contains("hello jarvis") || lower == "hello" || lower == "hi" -> "Hello Prince. JARVIS is ready."
            else -> "I heard: $command. This command is not mapped yet."
        }
    }

    private fun openAppOrWeb(context: MainActivity, packageName: String, url: String, spoken: String): String {
        val pm = context.packageManager
        val launch = pm.getLaunchIntentForPackage(packageName)
        if (launch != null) context.startActivity(launch) else context.openBrowser(url)
        return spoken
    }
}
