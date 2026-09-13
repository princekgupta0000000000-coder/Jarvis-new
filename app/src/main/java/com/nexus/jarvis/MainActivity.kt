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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
            Box(Modifier.weight(1f)) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good Evening, Prince", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("How can I help you today?", color = Muted, fontSize = 13.sp)
                }
                Icon(Icons.Rounded.Notifications, null, tint = Cyan)
            }
            Spacer(Modifier.height(10.dp))
            StatusChip()
            Text(spoken, color = Cyan, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
            AiCore { activity.listen(onVoice) }
            SectionTitle("TODAY'S OVERVIEW")
        }
        item { Overview("Next Class", "Physics • 9:00 AM", "Room 102", Icons.Rounded.School, Cyan) }
        item { Overview("Attendance", "78% overall", "Target 75% • calculate safe leaves", Icons.Rounded.CheckCircle, Green) { attendance = true } }
        item { Overview("Study Progress", "NEET • 62%", "3 tasks pending", Icons.Rounded.TaskAlt, Purple) }
        item {
            Spacer(Modifier.height(15.dp))
            SectionTitle("QUICK ACTIONS")
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Quick("Schedule", Icons.Rounded.CalendarMonth)
                Quick("Study", Icons.Rounded.PlayArrow)
                Quick("Call", Icons.Rounded.Phone)
                Quick("Browser", Icons.Rounded.Language)
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
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Green.copy(alpha = .09f)).border(1.dp, Green.copy(alpha = .22f), RoundedCornerShape(20.dp)).padding(horizontal = 11.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Green))
        Text("  JARVIS ONLINE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AiCore(click: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(.86f, 1.08f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxWidth().height(265.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(238.dp).alpha(.34f * pulse)) {
            drawCircle(Cyan, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            drawCircle(Blue, radius = size.minDimension * .40f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            repeat(12) { index ->
                val angle = Math.toRadians((index * 30).toDouble())
                val x = center.x + cos(angle).toFloat() * size.minDimension * .47f
                val y = center.y + sin(angle).toFloat() * size.minDimension * .47f
                drawCircle(Cyan, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Box(Modifier.size(138.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.34f), Blue.copy(.18f), Color.Transparent))).border(1.dp, Cyan.copy(.85f), CircleShape).clickable { click() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Mic, "Speak", tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Text("TAP TO SPEAK", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SectionTitle(text: String) = Text(text, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
fun Overview(title: String, value: String, subtitle: String, icon: ImageVector, iconColor: Color, click: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.055f), RoundedCornerShape(17.dp)).clickable(enabled = click != null) { click?.invoke() }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).clip(CircleShape).background(iconColor.copy(.11f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor) }
        Column(Modifier.padding(start = 13.dp)) {
            Text(title, color = Muted, fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = iconColor, fontSize = 10.sp)
        }
    }
}

@Composable
fun Quick(text: String, icon: ImageVector) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(5.dp))
        Text(text, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
fun SmallButton(text: String, modifier: Modifier, action: () -> Unit) {
    Button(onClick = action, modifier = modifier, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel2)) { Text(text, color = Color.White, fontSize = 10.sp) }
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
    AlertDialog(onDismissRequest = close, containerColor = Panel2, title = { Text("ATTENDANCE CALCULATOR", color = Cyan, fontWeight = FontWeight.Bold) }, text = {
        Column {
            Field("Present classes", present) { present = it }
            Field("Total classes", total) { total = it }
            Field("Target %", target) { target = it }
            Spacer(Modifier.height(8.dp))
            Text("Current: ${(current * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Safe classes you can miss: $safeMiss", color = Green)
        }
    }, confirmButton = { TextButton(onClick = close) { Text("DONE", color = Cyan) } })
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth().padding(vertical = 3.dp), label = { Text(label) }, singleLine = true)
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
        Text(day, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        rows.forEach { row ->
            Row(Modifier.padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(Cyan))
                Text(row, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
            }
        }
    }
}

@Composable
fun StudyScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("NEET + College Syllabus", "Track progress • Plan revision • Mock tests") }
        item { ProgressCard() }
        item { SubjectCard("Physics", "23 / 40 Chapters", .58f, Cyan) }
        item { SubjectCard("Chemistry", "26 / 40 Chapters", .65f, Purple) }
        item { SubjectCard("Biology", "29 / 40 Chapters", .73f, Green) }
        item { StudyTask("Human Reproduction", "Biology • 2 hours", true) }
        item { StudyTask("Organic Chemistry", "Chemistry • 1.5 hours", false) }
        item { StudyTask("Mechanics Revision", "Physics • 2 hours", false) }
    }
}

@Composable
fun ProgressCard() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("62%", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.padding(start = 17.dp)) {
            Text("Overall Progress", color = Muted, fontSize = 11.sp)
            Text("NEET Preparation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SubjectCard(name: String, detail: String, progress: Float, color: Color) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp), color = color, trackColor = Color.White.copy(.07f))
    }
}

@Composable
fun StudyTask(title: String, detail: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(15.dp)).background(Panel2).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (done) Green else Muted)
        Column(Modifier.padding(start = 11.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp)
            Text(detail, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun TasksScreen() {
    var tasks by remember { mutableStateOf(listOf("Revise Physics", "Biology mock test", "College assignment")) }
    var showAdd by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item {
            ScreenHeader("Tasks & Reminders", "Plan your day with JARVIS")
            Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan)) { Text("+ ADD TASK", color = Bg, fontWeight = FontWeight.Bold) }
        }
        items(tasks.size) { index ->
            Row(Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.TaskAlt, null, tint = Cyan)
                Text(tasks[index], color = Color.White, modifier = Modifier.weight(1f).padding(start = 10.dp))
                TextButton(onClick = { tasks = tasks.filterIndexed { i, _ -> i != index } }) { Text("DELETE", color = Muted, fontSize = 9.sp) }
            }
        }
    }
    if (showAdd) AddTaskDialog({ showAdd = false }) { value -> tasks = tasks + value; showAdd = false }
}

@Composable
fun AddTaskDialog(close: () -> Unit, add: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = close, containerColor = Panel2, title = { Text("ADD TASK", color = Cyan) }, text = { OutlinedTextField(value, { value = it }, label = { Text("Task") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (value.isNotBlank()) add(value) }) { Text("ADD", color = Cyan) } }, dismissButton = { TextButton(onClick = close) { Text("CANCEL", color = Muted) } })
}

@Composable
fun MoreScreen(activity: MainActivity) {
    var assistantOn by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("JARVIS Settings", "Assistant • Permissions • Safe actions") }
        item {
            SettingRow("24-Hour Assistant", if (assistantOn) "Foreground listening service active" else "Tap to enable background listening", Icons.Rounded.Mic, assistantOn) {
                assistantOn = it
                if (it) activity.startAssistantService() else activity.stopAssistantService()
            }
        }
        item { SettingRow("Wake Phrase", "Hello Jarvis", Icons.Rounded.Mic, false) {} }
        item { SettingRow("Notifications", "System notifications", Icons.Rounded.Notifications, false) {} }
        item { SettingRow("Device Settings", "Open Android settings", Icons.Rounded.Settings, false) { activity.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) } }
        item { InfoCard("SAFE ACTIONS", "Calls, messages and other sensitive actions open the Android confirmation UI. JARVIS does not bypass OTPs or CAPTCHAs.") }
    }
}

@Composable
fun SettingRow(title: String, detail: String, icon: ImageVector, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(43.dp).clip(CircleShape).background(Cyan.copy(.10f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Cyan) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, fontSize = 10.sp)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(bottom = 14.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun InfoCard(title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(Panel2).padding(14.dp)) {
        Text(title, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Panel2).padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        BottomItem(0, selected, "Home", Icons.Rounded.School, onSelect)
        BottomItem(1, selected, "Schedule", Icons.Rounded.CalendarMonth, onSelect)
        BottomItem(2, selected, "Study", Icons.Rounded.PlayArrow, onSelect)
        BottomItem(3, selected, "Tasks", Icons.Rounded.TaskAlt, onSelect)
        BottomItem(4, selected, "More", Icons.Rounded.Settings, onSelect)
    }
}

@Composable
fun BottomItem(index: Int, selected: Int, label: String, icon: ImageVector, onSelect: (Int) -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSelect(index) }.padding(horizontal = 10.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (selected == index) Cyan else Muted, modifier = Modifier.size(21.dp))
        Text(label, color = if (selected == index) Color.White else Muted, fontSize = 9.sp)
    }
}
