package com.nexus.jarvis

import android.os.Bundle
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF040914)
private val Panel = Color(0xFF091323)
private val Cyan = Color(0xFF19D8FF)
private val Blue = Color(0xFF317BFF)
private val Muted = Color(0xFF91A4BE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisApp() }
    }
}

@Composable
fun JarvisApp() {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(onVoice = { tab = 1 })
                    1 -> ScheduleScreen()
                    2 -> StudyScreen()
                    3 -> TasksScreen()
                    else -> SettingsScreen()
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun HomeScreen(onVoice: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good Evening, Prince", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("How can I help you today?", color = Muted, fontSize = 14.sp)
                }
                Icon(Icons.Rounded.Notifications, "Notifications", tint = Cyan, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(18.dp))
            AiCore(onClick = onVoice)
            Spacer(Modifier.height(20.dp))
            Text("TODAY'S OVERVIEW", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
        }
        item { OverviewCard("Next Class", "Physics • 9:00 AM", "Room 102", Icons.Rounded.School) }
        item { OverviewCard("Attendance", "78% overall", "2 classes can be missed", Icons.Rounded.CheckCircle) }
        item { OverviewCard("Study Progress", "NEET • 62%", "3 tasks pending", Icons.Rounded.TaskAlt) }
        item {
            Spacer(Modifier.height(16.dp))
            Text("QUICK ACTIONS", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Quick("Schedule", Icons.Rounded.CalendarMonth)
                Quick("Study", Icons.Rounded.PlayArrow)
                Quick("Call", Icons.Rounded.Phone)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun AiCore(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "core")
    val pulse by transition.animateFloat(0.82f, 1.08f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxWidth().height(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp).alpha(0.55f * pulse)) {
            drawCircle(Cyan, style = Stroke(2.dp.toPx()))
            drawCircle(Blue, radius = size.minDimension * .38f, style = Stroke(1.dp.toPx()))
        }
        Box(Modifier.size(130.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.35f), Blue.copy(.18f), Color.Transparent))).border(1.dp, Cyan.copy(.8f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Mic, "Activate JARVIS", tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Text("Listening...", color = Cyan, modifier = Modifier.align(Alignment.BottomCenter), fontSize = 13.sp)
    }
}

@Composable
fun OverviewCard(title: String, value: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Color.White.copy(.06f), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Cyan.copy(.12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Cyan) }
        Column(Modifier.padding(start = 13.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
fun Quick(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(6.dp)); Text(text, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
fun ScheduleScreen() { SimpleListScreen("College Schedule", listOf("Monday • Maths • 9:00 AM • Room 101", "Monday • Physics • 10:00 AM • Room 102", "Tuesday • Biology • 9:00 AM • Room 201", "Tuesday • Chemistry • 10:00 AM • Room 203")) }
@Composable
fun StudyScreen() { SimpleListScreen("NEET + College Study", listOf("Physics — Mechanics • 2 hours", "Chemistry — Organic • 1.5 hours", "Biology — Human Reproduction • 2 hours", "Revision — Mock Test • 45 minutes")) }
@Composable
fun TasksScreen() { SimpleListScreen("Tasks & Reminders", listOf("Physics assignment • Due today", "Study Organic Chemistry • Tomorrow", "College form fill • 2 days", "Gym • Daily 7:00 PM")) }
@Composable
fun SettingsScreen() { SimpleListScreen("Settings", listOf("Assistant Name • JARVIS", "Voice & Speech • Hello JARVIS", "Notifications • Enabled", "Permissions • Manage", "Data Backup • Local")) }

@Composable
fun SimpleListScreen(title: String, rows: List<String>) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp)) {
        item { Spacer(Modifier.height(25.dp)); Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(18.dp)) }
        items(rows) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(Cyan)); Text(row, color = Color.White, modifier = Modifier.padding(start = 13.dp), fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Rounded.Home, "Schedule" to Icons.Rounded.CalendarMonth, "Study" to Icons.Rounded.School, "Tasks" to Icons.Rounded.TaskAlt, "More" to Icons.Rounded.Settings)
    Row(Modifier.fillMaxWidth().background(Color(0xFF07101D)).padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        items.forEachIndexed { index, pair ->
            Column(Modifier.clickable { onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(pair.second, pair.first, tint = if (selected == index) Cyan else Muted, modifier = Modifier.size(23.dp))
                Text(pair.first, color = if (selected == index) Cyan else Muted, fontSize = 10.sp)
            }
        }
    }
}
