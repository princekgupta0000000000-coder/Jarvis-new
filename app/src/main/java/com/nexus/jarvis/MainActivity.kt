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

private val Bg=Color(0xFF030914); private val Panel=Color(0xFF091525); private val Panel2=Color(0xFF0D1B2D)
private val Cyan=Color(0xFF18D8FF); private val Blue=Color(0xFF327BFF); private val Purple=Color(0xFF7C5CFF); private val Green=Color(0xFF29E6A5); private val Muted=Color(0xFF91A4BE)

class MainActivity:ComponentActivity(){
 private var recognizer:SpeechRecognizer?=null; private var tts:TextToSpeech?=null
 override fun onCreate(b:Bundle?){super.onCreate(b); if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),41); tts=TextToSpeech(this){if(it==TextToSpeech.SUCCESS)tts?.language=Locale.US}; setContent{JarvisApp(this)}}
 fun speak(s:String){tts?.speak(s,TextToSpeech.QUEUE_FLUSH,null,"jarvis")}
 fun listen(onText:(String)->Unit){
  if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),41);return}
  if(!SpeechRecognizer.isRecognitionAvailable(this)){speak("Speech recognition is unavailable");return}
  recognizer?.destroy(); recognizer=SpeechRecognizer.createSpeechRecognizer(this)
  recognizer?.setRecognitionListener(object:android.speech.RecognitionListener{
   override fun onReadyForSpeech(p:Bundle?){}; override fun onBeginningOfSpeech(){}; override fun onRmsChanged(v:Float){}; override fun onBufferReceived(b:ByteArray?){}; override fun onEndOfSpeech(){}; override fun onPartialResults(b:Bundle?){}; override fun onEvent(t:Int,b:Bundle?){}
   override fun onError(e:Int){speak("I could not understand that")}
   override fun onResults(b:Bundle?){b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let{onText(it);speak(JarvisCommandRouter.execute(this@MainActivity,it))}}
  })
  recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())})
 }
 fun openBrowser(url:String)=startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(if(url.startsWith("http"))url else "https://$url")))
 fun dial(number:String)=startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${number.filter{it.isDigit()||it=='+'}}")))
 fun startAssistantService(){if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),42);if(android.os.Build.VERSION.SDK_INT>=26)startForegroundService(Intent(this,JarvisVoiceService::class.java))else startService(Intent(this,JarvisVoiceService::class.java))}
 fun stopAssistantService(){stopService(Intent(this,JarvisVoiceService::class.java))}
 override fun onDestroy(){recognizer?.destroy();tts?.shutdown();super.onDestroy()}
}

@Composable fun JarvisApp(a:MainActivity){var tab by remember{mutableIntStateOf(0)};var spoken by remember{mutableStateOf("Tap the core and say a command")};MaterialTheme(colorScheme=darkColorScheme(primary=Cyan,background=Bg,surface=Panel)){Column(Modifier.fillMaxSize().background(Bg)){Box(Modifier.weight(1f)){when(tab){0->Home(a,spoken){spoken=it};1->Schedule();2->Study();3->Tasks(a);else->More(a)}};BottomBar(tab){tab=it}}}}

@Composable fun Home(a:MainActivity,spoken:String,onVoice:(String)->Unit){var att by remember{mutableStateOf(false)};LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp)){item{Spacer(Modifier.height(24.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("Good Evening, Prince",color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold);Text("How can I help you today?",color=Muted,fontSize=13.sp)};Icon(Icons.Rounded.Notifications,null,tint=Cyan)};Spacer(Modifier.height(10.dp));StatusChip();Text(spoken,color=Cyan,fontSize=11.sp,modifier=Modifier.padding(top=7.dp));AiCore{a.listen(onVoice)};SectionTitle("TODAY'S OVERVIEW")};item{Overview("Next Class","Physics • 9:00 AM","Room 102",Icons.Rounded.School,Cyan)};item{Overview("Attendance","78% overall","Target 75% • calculate safe leaves",Icons.Rounded.CheckCircle,Green){att=true}};item{Overview("Study Progress","NEET • 62%","3 tasks pending",Icons.Rounded.TaskAlt,Purple)};item{Spacer(Modifier.height(15.dp));SectionTitle("QUICK ACTIONS");Spacer(Modifier.height(9.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Quick("Schedule",Icons.Rounded.CalendarMonth,Modifier.weight(1f));Quick("Study",Icons.Rounded.PlayArrow,Modifier.weight(1f));Quick("Call",Icons.Rounded.Phone,Modifier.weight(1f));Quick("Browser",Icons.Rounded.Language,Modifier.weight(1f))};Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){SmallButton("Open Google",Modifier.weight(1f)){a.openBrowser("google.com")};SmallButton("Dial 100",Modifier.weight(1f)){a.dial("100")}};Spacer(Modifier.height(22.dp))}};if(att)AttendanceDialog{att=false}}

@Composable fun StatusChip()=Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Green.copy(.09f)).border(1.dp,Green.copy(.22f),RoundedCornerShape(20.dp)).padding(horizontal=11.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(6.dp).clip(CircleShape).background(Green));Text("  JARVIS ONLINE",color=Green,fontSize=10.sp,fontWeight=FontWeight.Bold)}
@Composable fun AiCore(click:()->Unit){val t=rememberInfiniteTransition(label="core");val p by t.animateFloat(.86f,1.08f,infiniteRepeatable(tween(1300),RepeatMode.Reverse),label="pulse");Box(Modifier.fillMaxWidth().height(265.dp),contentAlignment=Alignment.Center){Canvas(Modifier.size(238.dp).alpha(.34f*p)){drawCircle(Cyan,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()));drawCircle(Blue,radius=size.minDimension*.40f,style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()));repeat(12){i->val x=Math.toRadians((i*30).toDouble());drawCircle(Cyan,2.5.dp.toPx(),androidx.compose.ui.geometry.Offset(center.x+kotlin.math.cos(x).toFloat()*size.minDimension*.47f,center.y+kotlin.math.sin(x).toFloat()*size.minDimension*.47f))}};Box(Modifier.size(138.dp).clip(CircleShape).background(Brush.radialGradient(listOf(Cyan.copy(.34f),Blue.copy(.18f),Color.Transparent))).border(1.dp,Cyan.copy(.85f),CircleShape).clickable{click()},contentAlignment=Alignment.Center){Icon(Icons.Rounded.Mic,"Speak",tint=Color.White,modifier=Modifier.size(44.dp))};Text("TAP TO SPEAK",color=Cyan,fontSize=11.sp,fontWeight=FontWeight.Bold,modifier=Modifier.align(Alignment.BottomCenter))}}
@Composable fun SectionTitle(s:String)=Text(s,color=Muted,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
@Composable fun Overview(t:String,v:String,s:String,i:ImageVector,c:Color,click:(()->Unit)?=null)=Row(Modifier.fillMaxWidth().padding(vertical=4.dp).clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp,Color.White.copy(.055f),RoundedCornerShape(17.dp)).clickable(enabled=click!=null){click?.invoke()}.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(45.dp).clip(CircleShape).background(c.copy(.11f)),contentAlignment=Alignment.Center){Icon(i,null,tint=c)};Column(Modifier.padding(start=13.dp)){Text(t,color=Muted,fontSize=11.sp);Text(v,color=Color.White,fontSize=15.sp,fontWeight=FontWeight.SemiBold);Text(s,color=c,fontSize=10.sp)}}}
@Composable fun Quick(t:String,i:ImageVector,m:Modifier)=Column(m.clip(RoundedCornerShape(14.dp)).background(Panel).padding(vertical=11.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(i,null,tint=Cyan,modifier=Modifier.size(23.dp));Spacer(Modifier.height(5.dp));Text(t,color=Color.White,fontSize=9.sp)}
@Composable fun SmallButton(t:String,m:Modifier,action:()->Unit)=Button(action,m,shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Panel2)){Text(t,color=Color.White,fontSize=10.sp)}
@Composable fun Field(l:String,v:String,set:(String)->Unit)=OutlinedTextField(v,set,Modifier.fillMaxWidth().padding(vertical=3.dp),label={Text(l)},singleLine=true)
@Composable fun AttendanceDialog(close:()->Unit){var p by remember{mutableStateOf("39")};var total by remember{mutableStateOf("50")};var target by remember{mutableStateOf("75")};val pp=p.toIntOrNull()?:0;val tt=total.toIntOrNull()?:0;val tg=(target.toDoubleOrNull()?:75.0)/100;val cur=if(tt>0)pp.toDouble()/tt else 0.0;val miss=if(tg>0&&pp>=tg*tt)floor((pp-tg*tt)/tg).toInt() else 0;AlertDialog(onDismissRequest=close,containerColor=Panel2,title={Text("ATTENDANCE CALCULATOR",color=Cyan,fontWeight=FontWeight.Bold)},text={Column{Field("Present classes",p){p=it};Field("Total classes",total){total=it};Field("Target %",target){target=it};Spacer(Modifier.height(8.dp));Text("Current: ${(cur*100).toInt()}%",color=Color.White,fontWeight=FontWeight.Bold);Text("Safe classes you can miss: $miss",color=Green)}},confirmButton={TextButton(onClick=close){Text("DONE",color=Cyan)}})}

@Composable fun Schedule(){LazyColumn(Modifier.fillMaxSize().padding(18.dp)){item{ScreenHeader("College Schedule","Timetable • Holidays • Working Days")};item{Day("MONDAY",listOf("Maths • 9:00 AM • Room 101","Physics • 10:00 AM • Room 102","English • 11:00 AM • Room 103"))};item{Day("TUESDAY",listOf("Biology • 9:00 AM • Room 201","Chemistry • 10:00 AM • Room 203","Lab • 12:00 PM • Lab 1"))};item{Day("WEDNESDAY",listOf("Physics • 9:00 AM • Room 102","Maths • 11:00 AM • Room 101"))};item{Info("HOLIDAYS","Mark holidays so JARVIS can calculate effective working days and attendance correctly.")}}}
@Composable fun Day(d:String,rows:List<String>)=Column(Modifier.fillMaxWidth().padding(vertical=5.dp).clip(RoundedCornerShape(17.dp)).background(Panel).padding(14.dp)){Text(d,color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold);rows.forEach{Row(Modifier.padding(top=11.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(7.dp).clip(CircleShape).background(Cyan));Text(it,color=Color.White,fontSize=13.sp,modifier=Modifier.padding(start=10.dp))}}}
@Composable fun Study()=LazyColumn(Modifier.fillMaxSize().padding(18.dp)){item{ScreenHeader("NEET + College Syllabus","Track progress • Plan revision • Mock tests")};item{ProgressCard()};item{Subject("Physics","23 / 40 Chapters",.58f,Cyan)};item{Subject("Chemistry","26 / 40 Chapters",.65f,Purple)};item{Subject("Biology","29 / 40 Chapters",.73f,Green)};item{StudyRow("Human Reproduction","Biology • 2 hours",true)};item{StudyRow("Organic Chemistry","Chemistry • 1.5 hours",false)};item{StudyRow("Mechanics Revision","Physics • 2 hours",false)}}
@Composable fun ProgressCard()=Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(17.dp),verticalAlignment=Alignment.CenterVertically){Text("62%",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Bold);Column(Modifier.padding(start=17.dp)){Text("Overall Progress",color=Muted,fontSize=11.sp);Text("NEET Preparation",color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold)}}
@Composable fun Subject(n:String,d:String,p:Float,c:Color)=Column(Modifier.fillMaxWidth().padding(top=10.dp).clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(n,color=Color.White,fontWeight=FontWeight.SemiBold);Text(d,color=Muted,fontSize=11.sp)};Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={p},modifier=Modifier.fillMaxWidth().height(5.dp),color=c,trackColor=Color.White.copy(.07f))}
@Composable fun StudyRow(t:String,s:String,done:Boolean)=Row(Modifier.fillMaxWidth().padding(top=8.dp).clip(RoundedCornerShape(15.dp)).background(Panel2).padding(13.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(done)Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,null,tint=if(done)Green else Muted);Column(Modifier.padding(start=11.dp)){Text(t,color=Color.White,fontSize=13.sp);Text(s,color=Muted,fontSize=10.sp)}}

@Composable fun Tasks(a:MainActivity){val store=remember{JarvisStore(a)};var tasks by remember{mutableStateOf(store.getTasks())};var add by remember{mutableStateOf(false)};LazyColumn(Modifier.fillMaxSize().padding(18.dp)){item{ScreenHeader("Tasks & Reminders","Plan your day with JARVIS");Button(onClick={add=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Cyan)){Text("+ ADD TASK",color=Bg,fontWeight=FontWeight.Bold)}};items(tasks.size){i->Row(Modifier.fillMaxWidth().padding(top=9.dp).clip(RoundedCornerShape(15.dp)).background(Panel).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.TaskAlt,null,tint=Cyan);Text(tasks[i],color=Color.White,modifier=Modifier.weight(1f).padding(start=10.dp));TextButton(onClick={tasks=tasks.filterIndexed{x,_ -> x!=i};store.setTasks(tasks)}){Text("DELETE",color=Muted,fontSize=9.sp)}}}};if(add)AddTaskDialog({add=false}){x->tasks=tasks+x;store.setTasks(tasks);add=false}}
@Composable fun AddTaskDialog(close:()->Unit,add:(String)->Unit){var v by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,containerColor=Panel2,title={Text("ADD TASK",color=Cyan)},text={OutlinedTextField(v,{v=it},label={Text("Task")},singleLine=true)},confirmButton={TextButton(onClick={if(v.isNotBlank())add(v)}){Text("ADD",color=Cyan)}},dismissButton={TextButton(onClick=close){Text("CANCEL",color=Muted)}})}

@Composable fun More(a:MainActivity){var on by remember{mutableStateOf(false)};LazyColumn(Modifier.fillMaxSize().padding(18.dp)){item{ScreenHeader("JARVIS Settings","Assistant • Permissions • Safe actions")};item{Setting("24-Hour Assistant",if(on)"Foreground listening service active" else "Tap to enable background wake listening",Icons.Rounded.RecordVoiceOver){if(on)a.stopAssistantService()else a.startAssistantService();on=!on}};item{Setting("Wake Phrase","Hello JARVIS • continuous listening mode",Icons.Rounded.Mic)};item{Setting("Notifications","Reminders and study alerts",Icons.Rounded.Notifications)};item{Setting("Browser","Open web links safely",Icons.Rounded.Language){a.openBrowser("google.com")}};item{Setting("Calls","Dialer opens for confirmation",Icons.Rounded.Phone){a.dial("100")}};item{Info("SAFE ACTIONS","JARVIS will not bypass OTP, CAPTCHA, authentication or website protections. Calls and messages remain under your control.")}}}
@Composable fun Setting(t:String,s:String,i:ImageVector,action:(()->Unit)?=null)=Row(Modifier.fillMaxWidth().padding(vertical=5.dp).clip(RoundedCornerShape(16.dp)).background(Panel).clickable(enabled=action!=null){action?.invoke()}.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(42.dp).clip(CircleShape).background(Cyan.copy(.1f)),contentAlignment=Alignment.Center){Icon(i,null,tint=Cyan)};Column(Modifier.padding(start=12.dp).weight(1f)){Text(t,color=Color.White,fontWeight=FontWeight.SemiBold);Text(s,color=Muted,fontSize=10.sp)};Icon(Icons.Rounded.ChevronRight,null,tint=Muted)}
@Composable fun Info(t:String,s:String)=Column(Modifier.fillMaxWidth().padding(top=10.dp).clip(RoundedCornerShape(17.dp)).background(Panel2).padding(15.dp)){Text(t,color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(6.dp));Text(s,color=Muted,fontSize=11.sp,lineHeight=17.sp)}
@Composable fun ScreenHeader(t:String,s:String)=Column(Modifier.padding(top=24.dp,bottom=14.dp)){Text(t,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold);Text(s,color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=4.dp))}
@Composable fun BottomBar(sel:Int,on:(Int)->Unit){val items=listOf("Home" to Icons.Rounded.Home,"Schedule" to Icons.Rounded.CalendarMonth,"Study" to Icons.Rounded.MenuBook,"Tasks" to Icons.Rounded.TaskAlt,"More" to Icons.Rounded.MoreHoriz);Row(Modifier.fillMaxWidth().background(Panel).padding(6.dp),horizontalArrangement=Arrangement.SpaceEvenly){items.forEachIndexed{i,p->Column(Modifier.weight(1f).clickable{on(i)},horizontalAlignment=Alignment.CenterHorizontally){Icon(p.second,null,tint=if(sel==i)Cyan else Muted);Text(p.first,color=if(sel==i)Cyan else Muted,fontSize=9.sp)}}}}