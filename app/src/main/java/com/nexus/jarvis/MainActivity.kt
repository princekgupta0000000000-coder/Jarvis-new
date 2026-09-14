package com.nexus.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Bg = Color(0xFF02060D)
private val Panel = Color(0xFF071321)
private val Panel2 = Color(0xFF0A1B2D)
private val Cyan = Color(0xFF18D8FF)
private val Blue = Color(0xFF237BFF)
private val Green = Color(0xFF25E6A0)
private val Muted = Color(0xFF8195AE)

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
        }
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US }
        setContent { JarvisApp(this) }
    }

    fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }

    fun listen(onText: (String) -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { speak("Speech recognition is unavailable"); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) { speak("I could not understand that") }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                onText(text)
                speak(JarvisCommandRouter.execute(this@MainActivity, text))
            }
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        })
    }

    fun openBrowser(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url"))) }
    }

    fun startAssistantService() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
        val i = Intent(this, JarvisVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }
    fun stopAssistantService() { stopService(Intent(this, JarvisVoiceService::class.java)) }

    override fun onDestroy() { recognizer?.destroy(); tts?.shutdown(); super.onDestroy() }
}

@Composable
fun JarvisApp(activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    var spoken by remember { mutableStateOf("SYSTEM READY • TAP CORE TO SPEAK") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> Home(activity, spoken) { spoken = it }
                    1 -> ScheduleScreen()
                    2 -> StudyScreen()
                    3 -> TasksScreen()
                    else -> MoreScreen(activity)
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun Home(activity: MainActivity, spoken: String, onVoice: (String) -> Unit) {
    var attendance by remember { mutableStateOf(false) }
    val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("J A R V I S", color = Cyan, fontSize = 22.sp)
                    Text("PERSONAL AI SYSTEM", color = Muted, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(time, color = Color.White, fontSize = 17.sp)
                    Text(date, color = Muted, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Green))
                Spacer(Modifier.width(7.dp))
                Text("ONLINE • LOCAL COMMAND ENGINE", color = Green, fontSize = 9.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(spoken, color = Cyan, fontSize = 10.sp)
            AiCore { activity.listen(onVoice) }
            SectionTitle("SYSTEM SNAPSHOT")
            Snapshot("NEXT CLASS", "Ask: \"aaj ki class kya hai\"")
            Snapshot("ATTENDANCE", "Open calculator → set your real numbers") { attendance = true }
            Snapshot("STUDY", "NEET + College progress")
            Spacer(Modifier.height(10.dp))
            SectionTitle("COMMAND DECK")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Action("YouTube") { activity.openBrowser("https://www.youtube.com") }
                Action("Google") { activity.openBrowser("https://www.google.com") }
                Action("Chrome") { activity.openBrowser("https://www.google.com") }
                Action("Search") { activity.openBrowser("https://www.google.com") }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
    if (attendance) AttendanceDialog { attendance = false }
}

@Composable
fun AiCore(click: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "hud")
    val pulse by transition.animateFloat(0.88f, 1.08f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(230.dp).alpha(.16f * pulse).border(1.dp, Cyan, CircleShape))
        Box(Modifier.size(185.dp).alpha(.3f * pulse).border(1.dp, Blue, CircleShape))
        Box(Modifier.size(142.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.55f), Blue.copy(.22f), Color.Transparent))).border(2.dp, Cyan.copy(.9f), CircleShape).clickable { click() }, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("J", color = Color.White, fontSize = 58.sp)
                Text("LISTEN", color = Cyan, fontSize = 9.sp)
            }
        }
        Text("TAP TO SPEAK", color = Muted, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable fun SectionTitle(text: String) = Text(text, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(vertical = 6.dp))

@Composable
fun Snapshot(title: String, text: String, click: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp, Cyan.copy(.07f), RoundedCornerShape(15.dp)).clickable(enabled = click != null) { click?.invoke() }.padding(13.dp)) {
        Text(title, color = Cyan, fontSize = 9.sp)
        Text(text, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun RowScope.Action(text: String, action: () -> Unit) {
    Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(11.dp)).background(Panel2).border(1.dp, Cyan.copy(.12f), RoundedCornerShape(11.dp)).clickable { action() }, contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
fun AttendanceDialog(close: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { JarvisStore(context) }
    var present by remember { mutableStateOf(store.getPresent().toString()) }
    var total by remember { mutableStateOf(store.getTotal().toString()) }
    var target by remember { mutableStateOf(store.getTarget().toString()) }
    val p = present.toIntOrNull() ?: 0
    val t = total.toIntOrNull() ?: 0
    val targetPct = (target.toDoubleOrNull() ?: 75.0).coerceIn(1.0, 100.0)
    val current = if (t > 0) p.toDouble() / t * 100 else 0.0
    val safeMiss = if (p.toDouble() / (t + 1) >= targetPct / 100) Int.MAX_VALUE else ((p - targetPct / 100 * t) / (targetPct / 100)).toInt().coerceAtLeast(0)
    AlertDialog(onDismissRequest = close, containerColor = Panel2, title = { Text("ATTENDANCE", color = Cyan) }, text = {
        Column {
            Field("Present classes", present) { present = it }
            Field("Total classes", total) { total = it }
            Field("Target %", target) { target = it }
            Spacer(Modifier.height(8.dp))
            Text("Current: ${"%.1f".format(current)}%", color = Color.White)
            Text(if (safeMiss == Int.MAX_VALUE) "You can miss additional classes while staying above target." else "Safe classes to miss: $safeMiss", color = Green)
        }
    }, confirmButton = { TextButton(onClick = { store.setAttendance(p, t, targetPct.toInt()); close() }) { Text("SAVE", color = Cyan) } })
}

@Composable fun Field(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, onChange, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), label = { Text(label) }, singleLine = true)

@Composable
fun ScheduleScreen() {
    val context = LocalContext.current
    val store = remember(context) { JarvisStore(context) }
    val rows = store.getSchedule()
    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("COLLEGE SCHEDULE", "Saved locally • ask JARVIS about today's classes") }
        days.forEach { day ->
            val dayRows = rows.filter { it.startsWith("$day|") }
            if (dayRows.isNotEmpty()) item { DayCard(day, dayRows) }
        }
        item { InfoCard("HOLIDAYS", "Holiday/working-day calculation is ready for stored timetable data; add your actual college holidays before relying on attendance predictions.") }
    }
}

@Composable
fun DayCard(day: String, rows: List<String>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
        Text(day, color = Cyan, fontSize = 9.sp)
        rows.forEach { raw ->
            val p = raw.split("|")
            Text("${p.getOrElse(1){"Class"}}  •  ${p.getOrElse(2){"--:--"}}  •  ${p.getOrElse(3){"Room"}}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
        }
    }
}

@Composable
fun StudyScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("STUDY HUB", "NEET + College") }
        item { ProgressCard("NEET", .62f) }
        item { ProgressCard("COLLEGE", .48f) }
        item { InfoCard("JARVIS MODE", "Use Tasks to build your study queue. Model inference can be added independently without changing your local data.") }
    }
}

@Composable
fun ProgressCard(title: String, progress: Float) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = Color.White); Text("${(progress * 100).toInt()}%", color = Cyan, fontSize = 11.sp) }
        Spacer(Modifier.height(9.dp)); LinearProgressIndicator({ progress }, Modifier.fillMaxWidth(), color = Cyan)
    }
}

@Composable
fun TasksScreen() {
    val context = LocalContext.current
    val store = remember(context) { JarvisStore(context) }
    var task by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(store.getTasks()) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("TASK MATRIX", "Persistent study tasks")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(task, { task = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("New task") })
            Spacer(Modifier.width(7.dp)); Button(onClick = { if (task.isNotBlank()) { tasks = tasks + task.trim(); store.setTasks(tasks); task = "" } }) { Text("ADD") }
        }
        tasks.forEachIndexed { index, value ->
            Row(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(12.dp)).background(Panel).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                TextButton(onClick = { tasks = tasks.filterIndexed { i, _ -> i != index }; store.setTasks(tasks) }) { Text("DONE", color = Cyan, fontSize = 9.sp) }
            }
        }
    }
}

@Composable
fun MoreScreen(activity: MainActivity) {
    var active by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("SYSTEM CONTROL", "Voice session and model")
        SettingRow("VOICE SESSION", "Foreground microphone session", active) { active = it; if (it) activity.startAssistantService() else activity.stopAssistantService() }
        SettingRow("WAKE PHRASE", "Hello Jarvis • Android/OEM may restrict always-on listening", false) { }
        InfoCard("LOCAL MODEL", "Your GGUF file is selected through the model picker. The current app keeps its URI locally; inference runtime integration is the next engine layer.")
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 13.sp); Text(subtitle, color = Muted, fontSize = 9.sp) }
        Switch(checked, onChange)
    }
}

@Composable fun ScreenHeader(title: String, subtitle: String) { Column(Modifier.padding(bottom = 10.dp)) { Text(title, color = Color.White, fontSize = 22.sp); Text(subtitle, color = Muted, fontSize = 11.sp) } }

@Composable
fun InfoCard(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
        Text(title, color = Cyan, fontSize = 9.sp); Text(text, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("CORE", "SCHEDULE", "STUDY", "TASKS", "SYSTEM")
    Row(Modifier.fillMaxWidth().background(Color(0xFF050D17)).padding(vertical = 4.dp)) {
        labels.forEachIndexed { i, label -> TextButton(onClick = { onSelect(i) }, Modifier.weight(1f)) { Text(label, color = if (i == selected) Cyan else Muted, fontSize = 8.sp) } }
    }
}