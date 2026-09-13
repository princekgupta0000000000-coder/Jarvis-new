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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.floor

private val Bg = Color(0xFF030914)
private val Panel = Color(0xFF091525)
private val Panel2 = Color(0xFF0D1B2D)
private val Cyan = Color(0xFF18D8FF)
private val Blue = Color(0xFF327BFF)
private val Purple = Color(0xFF7C5CFF)
private val Green = Color(0xFF29E6A5)
private val Muted = Color(0xFF91A4BE)

class MainActivity : ComponentActivity() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
        }
        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        setContent { JarvisApp(this) }
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    fun listen(onText: (String) -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is unavailable")
            return
        }
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        recognizer?.startListening(intent)
    }

    fun openBrowser(url: String) {
        val target = if (url.startsWith("http")) url else "https://$url"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
    }

    fun dial(number: String) {
        val clean = number.filter { it.isDigit() || it == '+' }
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")))
    }

    fun startAssistantService() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
        val intent = Intent(this, JarvisVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    fun stopAssistantService() {
        stopService(Intent(this, JarvisVoiceService::class.java))
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun JarvisApp(activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    var spoken by remember { mutableStateOf("Tap the core and say a command") }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Box(Modifier.fillMaxWidth().weight(1f)) {
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
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Spacer(Modifier.height(24.dp))
            Text("Good Evening, Prince", color = Color.White, fontSize = 23.sp)
            Text("How can I help you today?", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            StatusChip()
            Text(spoken, color = Cyan, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            AiCore { activity.listen(onVoice) }
            SectionTitle("TODAY'S OVERVIEW")
        }
        item { Overview("Next Class", "Physics • 9:00 AM", "Room 102") }
        item { Overview("Attendance", "78% overall", "Target 75% • calculate safe leaves") { attendance = true } }
        item { Overview("Study Progress", "NEET • 62%", "3 tasks pending") }
        item {
            Spacer(Modifier.height(15.dp))
            SectionTitle("QUICK ACTIONS")
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Quick("Schedule")
                Quick("Study")
                Quick("Call")
                Quick("Browser")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Open Google", Modifier.weight(1f)) { activity.openBrowser("google.com") }
                SmallButton("Dial 100", Modifier.weight(1f)) { activity.dial("100") }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
    if (attendance) AttendanceDialog { attendance = false }
}

@Composable
fun StatusChip() {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Green.copy(alpha = .09f)).border(1.dp, Green.copy(alpha = .22f), RoundedCornerShape(20.dp)).padding(horizontal = 11.dp, vertical = 5.dp)) {
        Text("JARVIS ONLINE", color = Green, fontSize = 10.sp)
    }
}

@Composable
fun AiCore(click: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(.86f, 1.08f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxWidth().height(265.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(238.dp).alpha(.25f * pulse).border(2.dp, Cyan, CircleShape))
        Box(Modifier.size(138.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.34f), Blue.copy(.18f), Color.Transparent))).border(1.dp, Cyan.copy(.85f), CircleShape).clickable { click() }, contentAlignment = Alignment.Center) {
            Text("MIC", color = Color.White, fontSize = 28.sp)
        }
        Text("TAP TO SPEAK", color = Cyan, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SectionTitle(text: String) = Text(text, color = Muted, fontSize = 10.sp)

@Composable
fun Overview(title: String, value: String, subtitle: String, click: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.055f), RoundedCornerShape(17.dp)).clickable(enabled = click != null) { click?.invoke() }.padding(15.dp)) {
        Text(title, color = Muted, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 15.sp)
        Text(subtitle, color = Cyan, fontSize = 10.sp)
    }
}

@Composable
fun Quick(text: String) {
    Box(Modifier.width(76.dp).height(58.dp).clip(RoundedCornerShape(14.dp)).background(Panel), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun SmallButton(text: String, modifier: Modifier, action: () -> Unit) {
    Button(onClick = action, modifier = modifier, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel2)) {
        Text(text, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun AttendanceDialog(close: () -> Unit) {
    var present by remember { mutableStateOf("39") }
    var total by remember { mutableStateOf("50") }
    var target by remember { mutableStateOf("75") }
    val p = present.toIntOrNull() ?: 0
    val t = total.toIntOrNull() ?: 0
    val targetValue = (target.toDoubleOrNull() ?: 75.0) / 100.0
    val current = if (t > 0) p.toDouble() / t else 0.0
    val safeMiss = if (targetValue > 0 && p >= targetValue * t) floor((p - targetValue * t) / targetValue).toInt() else 0
    AlertDialog(onDismissRequest = close, containerColor = Panel2, title = { Text("ATTENDANCE CALCULATOR", color = Cyan) }, text = {
        Column {
            Field("Present classes", present) { present = it }
            Field("Total classes", total) { total = it }
            Field("Target %", target) { target = it }
            Spacer(Modifier.height(8.dp))
            Text("Current: ${(current * 100).toInt()}%", color = Color.White)
            Text("Safe classes you can miss: $safeMiss", color = Green)
        }
    }, confirmButton = { TextButton(onClick = close) { Text("DONE", color = Cyan) } })
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), label = { Text(label) }, singleLine = true)
}

@Composable
fun ScheduleScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("College Schedule", "Timetable • Holidays • Working Days") }
        item { DayCard("MONDAY", listOf("Maths • 9:00 AM • Room 101", "Physics • 10:00 AM • Room 102", "English • 11:00 AM • Room 103")) }
        item { DayCard("TUESDAY", listOf("Biology • 9:00 AM • Room 201", "Chemistry • 10:00 AM • Room 203", "Lab • 12:00 PM • Lab 1")) }
        item { DayCard("WEDNESDAY", listOf("Physics • 9:00 AM • Room 102", "Maths • 11:00 AM • Room 101")) }
        item { InfoCard("HOLIDAYS", "Add holidays to calculate effective working days and attendance correctly.") }
    }
}

@Composable
fun DayCard(day: String, rows: List<String>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp)) {
        Text(day, color = Cyan, fontSize = 10.sp)
        rows.forEach { row -> Text("• $row", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)) }
    }
}

@Composable
fun StudyScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("Study Hub", "NEET + College syllabus") }
        item { ProgressCard("NEET", 0.62f) }
        item { ProgressCard("College", 0.48f) }
        item { InfoCard("TODAY", "Physics revision • Chemistry MCQs • Biology NCERT") }
    }
}

@Composable
fun ProgressCard(title: String, progress: Float) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text("${(progress * 100).toInt()}%", color = Cyan, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Cyan)
    }
}

@Composable
fun TasksScreen() {
    var task by remember { mutableStateOf("") }
    var tasks by remember { mutableStateOf(listOf("Revise Physics", "Solve Chemistry MCQs", "Read Biology NCERT")) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("Tasks", "Study plan and reminders")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = task, onValueChange = { task = it }, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("New task") })
            Spacer(Modifier.size(8.dp))
            Button(onClick = { if (task.isNotBlank()) { tasks = tasks + task.trim(); task = "" } }) { Text("ADD") }
        }
        tasks.forEachIndexed { index, value ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Panel).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = Color.White, modifier = Modifier.weight(1f))
                TextButton(onClick = { tasks = tasks.filterIndexed { i, _ -> i != index } }) { Text("DELETE", color = Cyan, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
fun MoreScreen(activity: MainActivity) {
    var active by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("JARVIS Settings", "Voice • Notifications • Assistant")
        SettingRow("24-hour assistant", "Foreground voice session", active) {
            active = it
            if (it) activity.startAssistantService() else activity.stopAssistantService()
        }
        SettingRow("Wake phrase", "Hello Jarvis", true) { }
        SettingRow("Notifications", "Assistant status", true) { }
    }
}

@Composable
fun SettingRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(title, color = Color.White, fontSize = 23.sp)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun InfoCard(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(15.dp)) {
        Text(title, color = Cyan, fontSize = 10.sp)
        Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("HOME", "SCHEDULE", "STUDY", "TASKS", "MORE")
    Row(Modifier.fillMaxWidth().background(Panel2).padding(vertical = 6.dp)) {
        labels.forEachIndexed { index, label ->
            TextButton(onClick = { onSelect(index) }, modifier = Modifier.weight(1f)) {
                Text(label, color = if (selected == index) Cyan else Muted, fontSize = 9.sp)
            }
        }
    }
}
