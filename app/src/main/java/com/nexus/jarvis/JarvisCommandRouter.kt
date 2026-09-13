package com.nexus.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings

/** Local-first command router. Sensitive actions open Android UI for user confirmation. */
object JarvisCommandRouter {
    fun execute(context: Context, raw: String): String {
        val command = raw.trim()
        val lower = command.lowercase()
        return when {
            lower.contains("open youtube") -> openAppOrWeb(context, "com.google.android.youtube", "https://youtube.com", "Opening YouTube")
            lower.contains("open chrome") -> openAppOrWeb(context, "com.android.chrome", "https://google.com", "Opening Chrome")
            lower.contains("open whatsapp") -> openAppOrWeb(context, "com.whatsapp", "https://web.whatsapp.com", "Opening WhatsApp")
            lower.contains("open google") || lower.contains("open browser") -> {
                openBrowser(context, "google.com"); "Opening Google"
            }
            lower.startsWith("call ") || lower.startsWith("dial ") -> {
                val number = command.substringAfter(' ').filter { it.isDigit() || it == '+' }
                if (number.isBlank()) "Please provide a phone number" else {
                    dial(context, number); "Opening the dialer for $number"
                }
            }
            lower.startsWith("text ") || lower.startsWith("sms ") -> {
                val message = command.substringAfter(' ', "").trim()
                if (message.isBlank()) "Tell me the message" else {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).apply {
                        putExtra("sms_body", message)
                    }
                    launch(context, intent)
                    "Opening messages for your confirmation"
                }
            }
            lower.contains("calendar") || lower.contains("add event") -> {
                launch(context, Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI))
                "Opening Calendar to add the event"
            }
            lower.contains("alarm") || lower.contains("remind me") -> {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM)
                if (intent.resolveActivity(context.packageManager) != null) launch(context, intent)
                "Opening the alarm and reminder screen"
            }
            lower.contains("settings") -> {
                launch(context, Intent(Settings.ACTION_SETTINGS)); "Opening Settings"
            }
            lower.contains("attendance") -> "Your attendance dashboard is available in JARVIS."
            lower.contains("who are you") || lower.contains("what can you do") ->
                "I am JARVIS. I can manage your schedule, study tasks, attendance, reminders, browser, apps and safe device actions."
            lower == "hello" || lower == "hi" -> "Hello Prince. JARVIS is ready."
            else -> "I heard: $command. This command is not mapped yet."
        }
    }

    private fun openAppOrWeb(context: Context, packageName: String, url: String, spoken: String): String {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) launch(context, launch) else openBrowser(context, url)
        return spoken
    }

    private fun openBrowser(context: Context, url: String) {
        launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url")))
    }

    private fun dial(context: Context, number: String) {
        launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${number.filter { it.isDigit() || it == '+' }}")))
    }

    private fun launch(context: Context, intent: Intent) {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
