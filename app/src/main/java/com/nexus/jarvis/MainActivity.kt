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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
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

private val Bg = Color(0xFF030914)
private val Panel = Color(0xFF091525)
private val Panel2 = Color(0xFF0D1B2D)
private val Cyan = Color(0xFF18D8FF)
private val Blue = Color(0xFF327BFF)
private val Purple = Color(0xFF7C5CFF)
private val Green = Color(0xFF29E6A5)
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
                    0 -> HomeScreen { tab = 1 }
                    1 -> ScheduleScreen()
                    2 -> StudyScreen()
                    3 -> TasksScreen()
                    else -> MoreScreen()
                }
            }
            BottomBar(tab) { tab = it }
        }
    }
}

@Composable
fun HomeScreen(onVoice: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Good Evening, Prince", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text("How can I help you today?", color = Muted, fontSize = 13.sp)
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Panel).border(1.dp, Cyan.copy(.25f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Notifications, "Notifications", tint = Cyan, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            StatusChip()
            Spacer(Modifier.height(8.dp))
            AiCore(onVoice)
            Spacer(Modifier.height(8.dp))
            SectionTitle("TODAY'S OVERVIEW")
        }
        item { OverviewCard("Next Class", "Physics • 9:00 AM", "Room 102", Icons.Rounded.School, Cyan) }
        item { OverviewCard("Attendance", "78% overall", "2 classes can be missed", Icons.Rounded.CheckCircle, Green) }
        item { OverviewCard("Study Progress", "NEET • 62%", "3 tasks pending", Icons.Rounded.TaskAlt, Purple) }
        item {
            Spacer(Modifier.height(16.dp))
            SectionTitle("QUICK ACTIONS")
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickAction("Schedule", Icons.Rounded.CalendarMonth)
                QuickAction("Study", Icons.Rounded.PlayArrow)
                QuickAction("Call", Icons.Rounded.Phone)
                QuickAction("More", Icons.Rounded.Apps)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
fun StatusChip() {
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Green.copy(.09f)).border(1.dp, Green.copy(.22f), RoundedCornerShape(20.dp)).padding(horizontal = 11.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Green))
        Text("  JARVIS ONLINE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AiCore(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "jarvis-core")
    val pulse by transition.animateFloat(0.88f, 1.08f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")
    Box(Modifier.fillMaxWidth().height(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(238.dp).alpha(.34f * pulse)) {
            drawCircle(Cyan, style = Stroke(1.5.dp.toPx()))
            drawCircle(Blue, radius = size.minDimension * .40f, style = Stroke(1.dp.toPx()))
            drawCircle(Cyan.copy(.35f), radius = size.minDimension * .30f, style = Stroke(2.dp.toPx()))
            for (i in 0 until 12) {
                val a = i * 30f
                val r = size.minDimension * .47f
                val x = center.x + kotlin.math.cos(Math.toRadians(a.toDouble())).toFloat() * r
                val y = center.y + kotlin.math.sin(Math.toRadians(a.toDouble())).toFloat() * r
                drawCircle(Cyan, radius = 2.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }
        Box(Modifier.size(138.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.34f), Blue.copy(.18f), Color.Transparent))).border(1.dp, Cyan.copy(.85f), CircleShape).clickable { onClick() }, contentAlignment = Alignment.Center) {
            Box(Modifier.size(104.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Blue.copy(.30f), Color.Transparent))).border(1.dp, Blue.copy(.55f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Mic, "Activate JARVIS", tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Listening...", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(18) { i -> Box(Modifier.size(2.dp, (5 + (i % 6) * 3).dp).clip(CircleShape).background(Cyan.copy(.8f))) }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) = Text(text, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

@Composable
fun OverviewCard(title: String, value: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color.White.copy(.055f), RoundedCornerShape(17.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(45.dp).clip(CircleShape).background(accent.copy(.11f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = accent, modifier = Modifier.size(23.dp)) }
        Column(Modifier.padding(start = 13.dp)) {
            Text(title, color = Muted, fontSize = 11.sp)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = accent.copy(.9f), fontSize = 10.sp)
        }
    }
}

@Composable
fun QuickAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, Color.White.copy(.05f), RoundedCornerShape(14.dp)).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(23.dp))
        Spacer(Modifier.height(5.dp)); Text(text, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
fun ScheduleScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("College Schedule", "Timetable • Holidays • Working Days"); WeekTabs() }
        item { DayCard("MONDAY", listOf("Maths", "Physics", "English"), listOf("9:00 AM", "10:00 AM", "11:00 AM")) }
        item { DayCard("TUESDAY", listOf("Biology", "Chemistry", "Lab"), listOf("9:00 AM", "10:00 AM", "12:00 PM")) }
        item { DayCard("WEDNESDAY", listOf("Physics", "Maths"), listOf("9:00 AM", "11:00 AM")) }
    }
}

@Composable
fun WeekTabs() {
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("Weekly", "Monthly").forEachIndexed { i, s -> Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (i == 0) Cyan.copy(.15f) else Panel).border(1.dp, if (i == 0) Cyan.copy(.4f) else Color.White.copy(.05f), RoundedCornerShape(10.dp)).padding(10.dp), contentAlignment = Alignment.Center) { Text(s, color = if (i == 0) Cyan else Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) } }
    }
}

@Composable
fun DayCard(day: String, subjects: List<String>, times: List<String>) {
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp)) {
        Text(day, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        subjects.forEachIndexed { i, subject -> Row(Modifier.fillMaxWidth().padding(top = 11.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).clip(CircleShape).background(Cyan)); Column(Modifier.padding(start = 10.dp)) { Text(subject, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text("${times[i]} • Room ${101 + i}", color = Muted, fontSize = 10.sp) } } }
    }
}

@Composable
fun StudyScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("NEET + College Syllabus", "Track progress • Plan revision • Mock tests") }
        item { ProgressCard() }
        item { Spacer(Modifier.height(10.dp)); SubjectProgress("Physics", "23 / 40 Chapters", .58f, Cyan) }
        item { SubjectProgress("Chemistry", "26 / 40 Chapters", .65f, Purple) }
        item { SubjectProgress("Biology", "29 / 40 Chapters", .73f, Green) }
        item { Spacer(Modifier.height(12.dp)); StudyItem("Human Reproduction", "Biology • 2 hours", true) }
        item { StudyItem("Organic Chemistry", "Chemistry • 1.5 hours", false) }
        item { StudyItem("Mechanics Revision", "Physics • 2 hours", false) }
    }
}

@Composable
fun ProgressCard() {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(86.dp), contentAlignment = Alignment.Center) { Canvas(Modifier.fillMaxSize()) { drawArc(Cyan, -90f, 223f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)); drawArc(Color.White.copy(.07f), 133f, 227f, false, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)) }; Text("62%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Column(Modifier.padding(start = 17.dp)) { Text("Overall Progress", color = Muted, fontSize = 11.sp); Text("NEET Preparation", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text("12 chapters this week", color = Green, fontSize = 10.sp) }
    }
}

@Composable
fun SubjectProgress(name: String, count: String, progress: Float, accent: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, color = Color.White, fontSize = 13.sp); Text(count, color = Muted, fontSize = 10.sp) }; Spacer(Modifier.height(7.dp)); Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Panel2)) { Box(Modifier.fillMaxWidth(progress).height(7.dp).clip(CircleShape).background(accent)) } }
}

@Composable
fun StudyItem(title: String, sub: String, done: Boolean) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.MenuBook, null, tint = if (done) Green else Cyan, modifier = Modifier.size(23.dp)); Column(Modifier.padding(start = 12.dp)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(sub, color = Muted, fontSize = 10.sp) } } }

@Composable
fun TasksScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("Tasks & Reminders", "Today • Pending • Completed") }
        item { TaskRow("Physics Assignment", "High • Due Today", Cyan) }
        item { TaskRow("Study Organic Chemistry", "Medium • Tomorrow", Purple) }
        item { TaskRow("College Form Fill", "Medium • 2 Days", Green) }
        item { TaskRow("Gym", "Daily • 7:00 PM", Cyan) }
        item { Spacer(Modifier.height(12.dp)); TaskRow("Mock Test Revision", "Sunday • 10:00 AM", Purple) }
    }
}

@Composable
fun TaskRow(title: String, sub: String, accent: Color) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(15.dp)).background(Panel).border(1.dp, Color.White.copy(.05f), RoundedCornerShape(15.dp)).padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(9.dp).clip(CircleShape).background(accent)); Column(Modifier.padding(start = 12.dp)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium); Text(sub, color = accent, fontSize = 10.sp) } } }

@Composable
fun MoreScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp)) {
        item { ScreenHeader("JARVIS Control Center", "Assistant • Phone Actions • Privacy") }
        item { SettingRow("Voice & Speech", "Hello JARVIS • Hands-free", Icons.Rounded.Mic) }
        item { SettingRow("Call & Message", "Safe confirmation before action", Icons.Rounded.Call) }
        item { SettingRow("Open Apps", "Launch installed apps", Icons.Rounded.Apps) }
        item { SettingRow("Open Link / Browser", "Chrome • Default browser", Icons.Rounded.Language) }
        item { SettingRow("Notifications", "Classes • Tasks • Study alerts", Icons.Rounded.Notifications) }
        item { SettingRow("Reminders", "Smart and recurring reminders", Icons.Rounded.Alarm) }
        item { SettingRow("Security & Privacy", "Local storage • Runtime permissions", Icons.Rounded.Security) }
        item { SettingRow("Settings", "Assistant name • Permissions", Icons.Rounded.Settings) }
    }
}

@Composable
fun SettingRow(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(43.dp).clip(CircleShape).background(Cyan.copy(.1f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Cyan) }; Column(Modifier.padding(start = 12.dp)) { Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(sub, color = Muted, fontSize = 10.sp) } } }

@Composable
fun ScreenHeader(title: String, subtitle: String) { Spacer(Modifier.height(22.dp)); Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)); Text(subtitle, color = Muted, fontSize = 11.sp); Spacer(Modifier.height(17.dp)) }

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf("Home" to Icons.Rounded.Home, "Schedule" to Icons.Rounded.CalendarMonth, "Study" to Icons.Rounded.School, "Tasks" to Icons.Rounded.TaskAlt, "More" to Icons.Rounded.Settings)
    Row(Modifier.fillMaxWidth().background(Color(0xFF06101D)).border(1.dp, Color.White.copy(.04f)).padding(top = 9.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
        items.forEachIndexed { index, pair ->
            Column(Modifier.clickable { onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(pair.second, pair.first, tint = if (selected == index) Cyan else Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp)); Text(pair.first, color = if (selected == index) Cyan else Muted, fontSize = 9.sp, fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
