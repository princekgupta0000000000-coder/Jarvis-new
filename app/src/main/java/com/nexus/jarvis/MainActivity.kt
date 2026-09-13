package com.nexus.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
        tts = TextToSpeech(this) { if (it == TextToSpeech.SUCCESS) tts?.language = Locale.US }
        setContent { JarvisApp(this) }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC)
        } else startAssistantService()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == REQ_MIC && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startAssistantService()
    }

    fun speak(text: String) { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }

    fun listen(onText: (String) -> Unit) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_MIC); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { speak("Speech recognition is unavailable"); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(v: Float) = Unit
            override fun onBufferReceived(b: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(b: Bundle?) = Unit
            override fun onEvent(t: Int, b: Bundle?) = Unit
            override fun onError(e: Int) = Unit
            override fun onResults(b: Bundle?) { b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { onText(it); speak(JarvisCommandRouter.execute(this@MainActivity, it)) } }
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to JARVIS")
        })
    }

    fun openBrowser(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url")))
    fun dial(number: String) = startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))

    fun startAssistantService() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFY)
        val intent = Intent(this, JarvisVoiceService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
    }
    fun stopAssistantService() = stopService(Intent(this, JarvisVoiceService::class.java))

    override fun onDestroy() { recognizer?.destroy(); tts?.stop(); tts?.shutdown(); super.onDestroy() }
    companion object { private const val REQ_MIC = 41; private const val REQ_NOTIFY = 42 }
}

@Composable
fun JarvisApp(activity: MainActivity) {
    var tab by remember { mutableIntStateOf(0) }
    var command by remember { mutableStateOf("24/7 wake listening is ready") }
    var dialog by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Bg, surface = Panel)) {
        Column(modifier = Modifier.fillMaxSize().background(Bg)) {
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    0 -> Home(activity, command) { command = it; dialog = true }
                    1 -> Schedule()
                    2 -> Study()
                    3 -> Tasks(activity)
                    else -> More(activity)
                }
            }
            BottomBar(tab) { tab = it }
        }
        if (dialog) AlertDialog(onDismissRequest = { dialog = false }, containerColor = Panel2, title = { Text("JARVIS COMMAND", color = Cyan, fontWeight = FontWeight.Bold) }, text = { Text(command, color = Color.White) }, confirmButton = { TextButton(onClick = { dialog = false }) { Text("OK", color = Cyan) } })
    }
}

@Composable
fun Home(activity: MainActivity, command: String, onVoice: (String) -> Unit) {
    var attendance by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Good Evening, Prince", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("How can I help you today?", color = Muted, fontSize = 13.sp) }
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Panel).border(1.dp, Cyan.copy(.25f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Notifications, null, tint = Cyan) }
            }
            Spacer(Modifier.height(10.dp)); StatusChip(); Text(command, color = Cyan, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp)); AiCore { activity.listen(onVoice) }; SectionTitle("TODAY'S OVERVIEW")
        }
        item { Overview("Next Class", "Physics • 9:00 AM", "Room 102", Icons.Rounded.School, Cyan) }
        item { Overview("Attendance", "78% overall", "Target 75% • calculate safe leaves", Icons.Rounded.CheckCircle, Green) { attendance = true } }
        item { Overview("Study Progress", "NEET • 62%", "3 tasks pending", Icons.Rounded.TaskAlt, Purple) }
        item {
            Spacer(Modifier.height(15.dp)); SectionTitle("QUICK ACTIONS"); Spacer(Modifier.height(9.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Quick("Schedule", Icons.Rounded.CalendarMonth, Modifier.weight(1f)); Quick("Study", Icons.Rounded.PlayArrow, Modifier.weight(1f)); Quick("Call", Icons.Rounded.Phone, Modifier.weight(1f)); Quick("Browser", Icons.Rounded.Language, Modifier.weight(1f)) }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { SmallButton("Open Google", Modifier.weight(1f)) { activity.openBrowser("google.com") }; SmallButton("Dial 100", Modifier.weight(1f)) { activity.dial("100") } }
            Spacer(Modifier.height(22.dp))
        }
    }
    if (attendance) AttendanceDialog { attendance = false }
}

@Composable fun StatusChip() = Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Green.copy(.09f)).border(1.dp, Green.copy(.22f), RoundedCornerShape(20.dp)).padding(horizontal = 11.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Green)); Text("  JARVIS ONLINE • WAKE LISTENING", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold) }

@Composable
fun AiCore(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(.86f, 1.08f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")
    Box(modifier = Modifier.fillMaxWidth().height(265.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(238.dp).alpha(.34f * pulse)) {
            drawCircle(Cyan, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())); drawCircle(Blue, radius = size.minDimension * .40f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())); drawCircle(Cyan.copy(.35f), radius = size.minDimension * .30f, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            repeat(12) { i -> val a = Math.toRadians((i * 30).toDouble()); drawCircle(Cyan, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(a).toFloat() * size.minDimension * .47f, center.y + kotlin.math.sin(a).toFloat() * size.minDimension * .47f)) }
        }
        Box(modifier = Modifier.size(138.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.34f), Blue.copy(.18f), Color.Transparent))).border(1.dp, Cyan.copy(.85f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(104.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Blue.copy(.30f), Color.Transparent))).border(1.dp, Blue.copy(.55f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Mic, "Speak", tint = Color.White, modifier = Modifier.size(44.dp)) } }
        Text("TAP TO SPEAK", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable fun SectionTitle(t: String) = Text(t, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
@Composable fun Overview(title: String, value: String, sub: String, icon: ImageVector, accent: Color, click: (() -> Unit)? = null) = Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.055f), RoundedCornerShape(17.dp)).clickable(enabled = click != null) { click?.invoke() }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(45.dp).clip(CircleShape).background(accent.copy(.11f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent) }; Column(modifier = Modifier.padding(start = 13.dp)) { Text(title, color = Muted, fontSize = 11.sp); Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(sub, color = accent.copy(.9f), fontSize = 10.sp) } }
@Composable fun Quick(t: String, i: ImageVector, modifier: Modifier = Modifier) = Column(modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Panel).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(i, null, tint = Cyan, modifier = Modifier.size(23.dp)); Spacer(Modifier.height(5.dp)); Text(t, color = Color.White, fontSize = 9.sp) }
@Composable fun SmallButton(t: String, modifier: Modifier = Modifier, action: () -> Unit) = Button(onClick = action, modifier = modifier, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel2)) { Text(t, color = Color.White, fontSize = 10.sp) }

@Composable
fun AttendanceDialog(close: () -> Unit) {
    var present by remember { mutableStateOf("39") }; var total by remember { mutableStateOf("50") }; var target by remember { mutableStateOf("75") }
    val p = present.toIntOrNull() ?: 0; val tt = total.toIntOrNull() ?: 0; val tg = (target.toDoubleOrNull() ?: 75.0) / 100.0; val current = if (tt > 0) p.toDouble() / tt else 0.0; val misses = if (tg > 0 && p >= tg * tt) floor((p - tg * tt) / tg).toInt() else 0
    AlertDialog(onDismissRequest = close, containerColor = Panel2, title = { Text("ATTENDANCE CALCULATOR", color = Cyan, fontWeight = FontWeight.Bold) }, text = { Column { Field("Present classes", present) { present = it }; Field("Total classes", total) { total = it }; Field("Target %", target) { target = it }; Spacer(Modifier.height(10.dp)); Text("Current: ${(current * 100).toInt()}%", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("Safe classes you can miss: $misses", color = Green, fontSize = 13.sp) } }, confirmButton = { TextButton(onClick = close) { Text("DONE", color = Cyan) } })
}
@Composable fun Field(label: String, value: String, set: (String) -> Unit) = OutlinedTextField(value = value, onValueChange = set, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), label = { Text(label) }, singleLine = true)

@Composable fun Schedule() = LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) { item { ScreenHeader("College Schedule", "Timetable • Holidays • Working Days"); TogglePills() }; item { Day("MONDAY", listOf("Maths • 9:00 AM • Room 101", "Physics • 10:00 AM • Room 102", "English • 11:00 AM • Room 103")) }; item { Day("TUESDAY", listOf("Biology • 9:00 AM • Room 201", "Chemistry • 10:00 AM • Room 203", "Lab • 12:00 PM • Lab 1")) }; item { Day("WEDNESDAY", listOf("Physics • 9:00 AM • Room 102", "Maths • 11:00 AM • Room 101")) }; item { Info("HOLIDAYS", "Mark holidays so JARVIS can calculate effective working days and attendance correctly.") } }
@Composable fun TogglePills() = Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Weekly", "Monthly").forEachIndexed { i, s -> Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (i == 0) Cyan.copy(.15f) else Panel).padding(10.dp), contentAlignment = Alignment.Center) { Text(s, color = if (i == 0) Cyan else Muted, fontSize = 11.sp) } } }
@Composable fun Day(day: String, rows: List<String>) = Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp)) { Text(day, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold); rows.forEach { Row(modifier = Modifier.padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Cyan)); Text(it, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp)) } } }

@Composable fun Study() = LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) { item { ScreenHeader("NEET + College Syllabus", "Track progress • Plan revision • Mock tests") }; item { ProgressCard() }; item { Subject("Physics", "23 / 40 Chapters", .58f, Cyan) }; item { Subject("Chemistry", "26 / 40 Chapters", .65f, Purple) }; item { Subject("Biology", "29 / 40 Chapters", .73f, Green) }; item { StudyRow("Human Reproduction", "Biology • 2 hours", true) }; item { StudyRow("Organic Chemistry", "Chemistry • 1.5 hours", false) }; item { StudyRow("Mechanics Revision", "Physics • 2 hours", false) }; item { StudyRow("Mock Test", "120 questions • 45 min", false) } }
@Composable fun ProgressCard() = Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(82.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { .62f }, modifier = Modifier.fillMaxSize(), color = Cyan, trackColor = Color.White.copy(.07f), strokeWidth = 7.dp); Text("62%", color = Color.White, fontWeight = FontWeight.Bold) }; Column(modifier = Modifier.padding(start = 17.dp)) { Text("Overall Progress", color = Muted, fontSize = 11.sp); Text("NEET Preparation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold); Text("College + NEET combined", color = Cyan, fontSize = 10.sp) } }
@Composable fun Subject(name: String, detail: String, progress: Float, accent: Color) = Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, color = Color.White, fontWeight = FontWeight.SemiBold); Text(detail, color = Muted, fontSize = 11.sp) }; Spacer(Modifier.height(8.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)), color = accent, trackColor = Color.White.copy(.07f)) }
@Composable fun StudyRow(title: String, sub: String, done: Boolean) = Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(15.dp)).background(Panel2).padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = if (done) Green else Muted); Column(modifier = Modifier.padding(start = 11.dp)) { Text(title, color = Color.White, fontSize = 13.sp); Text(sub, color = Muted, fontSize = 10.sp) } }

@Composable
fun Tasks(activity: MainActivity) {
    val store = remember { JarvisStore(activity) }; var tasks by remember { mutableStateOf(store.getTasks()) }; var add by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("Tasks & Reminders", "Plan your day with JARVIS"); Spacer(Modifier.height(10.dp)); Button(onClick = { add = true }, colors = ButtonDefaults.buttonColors(containerColor = Cyan), modifier = Modifier.fillMaxWidth()) { Text("+ ADD TASK", color = Bg, fontWeight = FontWeight.Bold) } }
        items(tasks.size) { index -> val task = tasks[index]; Row(modifier = Modifier.fillMaxWidth().padding(top = 9.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.TaskAlt, null, tint = Cyan); Text(task, color = Color.White, modifier = Modifier.weight(1f).padding(start = 10.dp)); TextButton(onClick = { tasks = tasks.filterIndexed { i, _ -> i != index }; store.setTasks(tasks) }) { Text("DELETE", color = Muted, fontSize = 9.sp) } } }
    }
    if (add) AddTaskDialog({ add = false }) { task -> tasks = tasks + task; store.setTasks(tasks); add = false }
}
@Composable fun AddTaskDialog(onClose: () -> Unit, onAdd: (String) -> Unit) { var value by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onClose, containerColor = Panel2, title = { Text("ADD TASK", color = Cyan) }, text = { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Task") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onAdd(value) }) { Text("ADD", color = Cyan) } }, dismissButton = { TextButton(onClick = onClose) { Text("CANCEL", color = Muted) } }) }

@Composable
fun More(activity: MainActivity) {
    var on by remember { mutableStateOf(true) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("JARVIS Settings", "Assistant • Wake Phrase • Safe actions") }
        item { Setting("24/7 Assistant", if (on) "Foreground microphone + wake listening ON" else "Wake listening OFF", Icons.Rounded.RecordVoiceOver) { if (on) activity.stopAssistantService() else activity.startAssistantService(); on = !on } }
        item { Setting("Wake Phrase", "Say: Hello JARVIS", Icons.Rounded.Mic) }
        item { Setting("Notifications", "Reminders and study alerts", Icons.Rounded.Notifications) }
        item { Setting("App Launcher", "Open installed apps with Android intents", Icons.Rounded.Apps) }
        item { Setting("Browser", "Open web links safely", Icons.Rounded.Language) { activity.openBrowser("google.com") } }
        item { Setting("Calls", "Dialer opens for confirmation", Icons.Rounded.Phone) { activity.dial("100") } }
        item { Setting("Privacy", "No OTP/CAPTCHA/authentication bypass", Icons.Rounded.Security) }
        item { Info("24/7 WAKE MODE", "JARVIS keeps a foreground microphone session alive and continuously restarts speech recognition. Say Hello JARVIS, then speak your command. Android/OEM battery restrictions may still stop long-running background services.") }
    }
}
@Composable fun Setting(title: String, sub: String, icon: ImageVector, action: (() -> Unit)? = null) = Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(Panel).clickable(enabled = action != null) { action?.invoke() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Cyan.copy(.1f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Cyan) }; Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(sub, color = Muted, fontSize = 10.sp) }; Icon(Icons.Rounded.ChevronRight, null, tint = Muted) }
@Composable fun Info(title: String, text: String) = Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(17.dp)).background(Panel2).border(1.dp, Cyan.copy(.12f), RoundedCornerShape(17.dp)).padding(15.dp)) { Text(title, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(text, color = Muted, fontSize = 11.sp, lineHeight = 17.sp) }
@Composable fun ScreenHeader(title: String, sub: String) = Column(modifier = Modifier.padding(top = 24.dp, bottom = 14.dp)) { Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text(sub, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Rounded.Home, "Schedule" to Icons.Rounded.CalendarMonth, "Study" to Icons.Rounded.MenuBook, "Tasks" to Icons.Rounded.TaskAlt, "More" to Icons.Rounded.MoreHoriz)
    Row(modifier = Modifier.fillMaxWidth().background(Panel).padding(vertical = 7.dp)) {
        items.forEachIndexed { index, pair ->
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { onSelect(index) }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(pair.second, null, tint = if (selected == index) Cyan else Muted, modifier = Modifier.size(21.dp)); Text(pair.first, color = if (selected == index) Color.White else Muted, fontSize = 8.sp) }
        }
    }
}
