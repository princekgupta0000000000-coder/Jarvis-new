package com.nexus.jarvis

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import java.util.Locale

/** Finds any user-installed launchable app by its visible launcher label. */
object JarvisAppLauncher {
    fun tryOpen(activity: Activity, raw: String): String? {
        val lower = raw.lowercase(Locale.US).trim()
        val prefix = when {
            lower.startsWith("open ") -> "open "
            lower.startsWith("launch ") -> "launch "
            lower.startsWith("start ") -> "start "
            lower.endsWith(" kholo") -> ""
            lower.endsWith(" khol") -> ""
            else -> return null
        }
        val query = lower.removePrefix(prefix).removeSuffix(" kholo").removeSuffix(" khol").trim()
        if (query.isBlank() || query in setOf("browser", "google", "settings", "camera", "phone", "youtube", "chrome", "whatsapp", "maps", "gmail", "calendar", "messages")) return null

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = activity.packageManager.queryIntentActivities(launcherIntent, 0).firstOrNull { info ->
            val label = info.loadLabel(activity.packageManager).toString().lowercase(Locale.US)
            label == query || label.contains(query) || query.contains(label)
        } ?: return null

        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).apply {
            component = ComponentName(match.activityInfo.packageName, match.activityInfo.name)
        }
        activity.startActivity(launch)
        return match.loadLabel(activity.packageManager).toString()
    }
}
