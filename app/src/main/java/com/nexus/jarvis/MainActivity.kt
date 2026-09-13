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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
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
            if (result == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        setContent {
            JarvisApp(this)
        }
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

            override fun onError(error: Int) {
                speak("I could not understand that")
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?: return

                onText(text)
                speak(JarvisCommandRouter.execute(this@MainActivity, text))
            }
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
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
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }

        val serviceIntent = Intent(this, JarvisVoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
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

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            background = Bg,
            surface = Panel
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (tab) {
                    0 -> Home(activity, spoken) { spoken = it }
                    1 -> Schedule()
                    2 -> Study()
                    3 -> Tasks(activity)
                    else -> More(activity)
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun Home(
    activity: MainActivity,
    spoken: String,
    onVoice: (String) -> Unit
) {
    var showAttendance by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Good Evening, Prince",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "How can I help you today?",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
                Icon(Icons.Rounded.Notifications, null, tint = Cyan)
            }

            Spacer(Modifier.height(10.dp))
            StatusChip()
            Text(
                spoken,
                color = Cyan,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 7.dp)
            )
            AiCore { activity.listen(onVoice) }
            SectionTitle("TODAY'S OVERVIEW")
        }

        item {
            Overview(
                "Next Class",
                "Physics • 9:00 AM",
                "Room 102",
                Icons.Rounded.School,
                Cyan
            )
        }
        item {
            Overview(
                "Attendance",
                "78% overall",
                "Target 75% • calculate safe leaves",
                Icons.Rounded.CheckCircle,
                Green
            ) { showAttendance = true }
        }
        item {
            Overview(
                "Study Progress",
                "NEET • 62%",
                "3 tasks pending",
                Icons.Rounded.TaskAlt,
                Purple
            )
        }

        item {
            Spacer(Modifier.height(15.dp))
            SectionTitle("QUICK ACTIONS")
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Quick("Schedule", Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                Quick("Study", Icons.Rounded.PlayArrow, Modifier.weight(1f))
                Quick("Call", Icons.Rounded.Phone, Modifier.weight(1f))
                Quick("Browser", Icons.Rounded.Language, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallButton("Open Google", Modifier.weight(1f)) {
                    activity.openBrowser("google.com")
                }
                SmallButton("Dial 100", Modifier.weight(1f)) {
                    activity.dial("100")
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }

    if (showAttendance) {
        AttendanceDialog { showAttendance = false }
    }
}

@Composable
fun StatusChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Green.copy(alpha = 0.09f))
            .border(1.dp, Green.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
            .padding(horizontal = 11.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Green)
        )
        Text(
            "  JARVIS ONLINE",
            color = Green,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AiCore(click: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(265.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(238.dp)
                .alpha(0.34f * pulse)
        ) {
            drawCircle(
                color = Cyan,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
            )
            drawCircle(
                color = Blue,
                radius = size.minDimension * 0.40f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
            )

            repeat(12) { index ->
                val angle = Math.toRadians((index * 30).toDouble())
                val x = center.x + cos(angle).toFloat() * size.minDimension * 0.47f
                val y = center.y + sin(angle).toFloat() * size.minDimension * 0.47f
                drawCircle(Cyan, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
            }
        }

        Box(
            modifier = Modifier
                .size(138.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Cyan.copy(alpha = 0.34f),
                            Blue.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .border(1.dp, Cyan.copy(alpha = 0.85f), CircleShape)
                .clickable { click() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Mic,
                contentDescription = "Speak",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }

        Text(
            "TAP TO SPEAK",
            color = Cyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        color = Muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun Overview(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    click: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Panel)
            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(17.dp))
            .clickable(enabled = click != null) { click?.invoke() }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor)
        }
        Column(modifier = Modifier.padding(start = 13.dp)) {
            Text(title, color = Muted, fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = iconColor, fontSize = 10.sp)
        }
    }
}

@Composable
fun Quick(text: String, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Panel)
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(5.dp))
        Text(text, color = Color.White, fontSize = 9.sp)
    }
}

@Composable
fun SmallButton(text: String, modifier: Modifier, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Panel2)
    ) {
        Text(text, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun Field(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
fun AttendanceDialog(close: () -> Unit) {
    var present by remember { mutableStateOf("39") }
    var total by remember { mutableStateOf("50") }
    var target by remember { mutableStateOf("75") }

    val presentCount = present.toIntOrNull() ?: 0
    val totalCount = total.toIntOrNull() ?: 0
    val targetRatio = (target.toDoubleOrNull() ?: 75.0) / 100.0
    val current = if (totalCount > 0) presentCount.toDouble() / totalCount else 0.0
    val safeMisses = if (targetRatio > 0 && presentCount >= targetRatio * totalCount) {
        floor((presentCount - targetRatio * totalCount) / targetRatio).toInt()
    } else {
        0
    }

    AlertDialog(
        onDismissRequest = close,
        containerColor = Panel2,
        title = {
            Text(
                "ATTENDANCE CALCULATOR",
                color = Cyan,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Field("Present classes", present) { present = it }
                Field("Total classes", total) { total = it }
                Field("Target %", target) { target = it }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Current: ${(current * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Safe classes you can miss: $safeMisses",
                    color = Green
                )
            }
        },
        confirmButton = {
            TextButton(onClick = close) {
                Text("DONE", color = Cyan)
            }
        }
    )
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun Schedule() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        item { ScreenHeader("College Schedule", "Timetable • Holidays • Working Days") }
        item { Day("MONDAY", listOf("Maths • 9:00 AM • Room 101", "Physics • 10:00 AM • Room 102", "English • 11:00 AM • Room 103")) }
        item { Day("TUESDAY", listOf("Biology • 9:00 AM • Room 201", "Chemistry • 10:00 AM • Room 203", "Lab • 12:00 PM • Lab 1")) }
        item { Day("WEDNESDAY", listOf("Physics • 9:00 AM • Room 102", "Maths • 11:00 AM • Room 101")) }
        item { Info("HOLIDAYS", "Mark holidays so JARVIS can calculate effective working days and attendance correctly.") }
    }
}

@Composable
fun Day(day: String, rows: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Panel)
            .padding(14.dp)
    ) {
        Text(day, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        rows.forEach { row ->
            Row(
                modifier = Modifier.padding(top = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Cyan)
                )
                Text(
                    row,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
fun Info(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel2)
            .padding(14.dp)
    ) {
        Text(title, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(text, color = Muted, fontSize = 12.sp)
    }
}

@Composable
fun Study() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        item { ScreenHeader("NEET + College Syllabus", "Track progress • Plan revision • Mock tests") }
        item { ProgressCard() }
        item { Subject("Physics", "23 / 40 Chapters", 0.58f, Cyan) }
        item { Subject("Chemistry", "26 / 40 Chapters", 0.65f, Purple) }
        item { Subject("Biology", "29 / 40 Chapters", 0.73f, Green) }
        item { StudyRow("Human Reproduction", "Biology • 2 hours", true) }
        item { StudyRow("Organic Chemistry", "Chemistry • 1.5 hours", false) }
        item { StudyRow("Mechanics Revision", "Physics • 2 hours", false) }
    }
}

@Composable
fun ProgressCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Panel)
            .padding(17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("62%", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Column(modifier = Modifier.padding(start = 17.dp)) {
            Text("Overall Progress", color = Muted, fontSize = 11.sp)
            Text("NEET Preparation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Subject(name: String, detail: String, progress: Float, color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            color = color,
            trackColor = Color.White.copy(alpha = 0.07f)
        )
    }
}

@Composable
fun StudyRow(title: String, subtitle: String, done: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Panel2)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            tint = if (done) Green else Muted
        )
        Column(modifier = Modifier.padding(start = 11.dp)) {
            Text(title, color = Color.White, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun Tasks(activity: MainActivity) {
    val store = remember { JarvisStore(activity) }
    var tasks by remember { mutableStateOf(store.getTasks()) }
    var addTask by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        item {
            ScreenHeader("Tasks & Reminders", "Plan your day with JARVIS")
            Button(
                onClick = { addTask = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan)
            ) {
                Text("+ ADD TASK", color = Bg, fontWeight = FontWeight.Bold)
            }
        }

        itemsIndexed(tasks) { index, task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Panel)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.TaskAlt, null, tint = Cyan)
                Text(
                    task,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                )
                TextButton(
                    onClick = {
                        tasks = tasks.filterIndexed { itemIndex, _ -> itemIndex != index }
                        store.setTasks(tasks)
                    }
                ) {
                    Icon(Icons.Rounded.Delete, null, tint = Muted)
                }
            }
        }
    }

    if (addTask) {
        AddTaskDialog(
            close = { addTask = false },
            add = { value ->
                tasks = tasks + value
                store.setTasks(tasks)
                addTask = false
            }
        )
    }
}

@Composable
fun AddTaskDialog(close: () -> Unit, add: (String) -> Unit) {
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = close,
        containerColor = Panel2,
        title = { Text("ADD TASK", color = Cyan) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Task") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) add(value) }) {
                Text("ADD", color = Cyan)
            }
        },
        dismissButton = {
            TextButton(onClick = close) {
                Text("CANCEL", color = Muted)
            }
        }
    )
}

@Composable
fun More(activity: MainActivity) {
    var assistantOn by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        item {
            ScreenHeader("JARVIS Settings", "Assistant • Permissions • Safe actions")
        }

        item {
            SettingRow(
                "24-Hour Assistant",
                if (assistantOn) "Foreground listening service active" else "Enable background wake listening",
                Icons.Rounded.Mic,
                assistantOn
            ) { enabled ->
                assistantOn = enabled
                if (enabled) activity.startAssistantService() else activity.stopAssistantService()
            }
        }

        item {
            SettingRow(
                "Wake Phrase",
                "Hello Jarvis",
                Icons.Rounded.Mic,
                true
            ) { }
        }

        item {
            SettingRow(
                "Notifications",
                "System notifications for assistant service",
                Icons.Rounded.Notifications,
                true
            ) { }
        }

        item {
            SettingRow(
                "Browser & Apps",
                "Open supported apps and web pages",
                Icons.Rounded.Language,
                true
            ) { }
        }

        item {
            SettingRow(
                "Safe Device Actions",
                "Dialer, SMS and calendar use system confirmation screens",
                Icons.Rounded.Settings,
                true
            ) { }
        }

        item {
            Spacer(Modifier.height(10.dp))
            Info(
                "IMPORTANT",
                "Android may restrict continuous microphone access because of battery and privacy rules. The assistant uses a foreground service when enabled; it cannot bypass Android restrictions."
            )
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Panel)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(43.dp)
                .clip(CircleShape)
                .background(Cyan.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Cyan)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun BottomBar(selected: Int, onSelected: (Int) -> Unit) {
    val labels = listOf("HOME", "SCHEDULE", "STUDY", "TASKS", "MORE")
    val icons = listOf(
        Icons.Rounded.Mic,
        Icons.Rounded.CalendarMonth,
        Icons.Rounded.School,
        Icons.Rounded.TaskAlt,
        Icons.Rounded.Settings
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel2)
            .navigationBarsPadding()
            .padding(horizontal = 5.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        labels.forEachIndexed { index, label ->
            val active = selected == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelected(index) }
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icons[index],
                    contentDescription = label,
                    tint = if (active) Cyan else Muted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    label,
                    color = if (active) Cyan else Muted,
                    fontSize = 8.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
