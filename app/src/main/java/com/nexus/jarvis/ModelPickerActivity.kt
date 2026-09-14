package com.nexus.jarvis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ModelBg = Color(0xFF030914)
private val ModelPanel = Color(0xFF091525)
private val ModelCyan = Color(0xFF18D8FF)
private val ModelMuted = Color(0xFF91A4BE)

class ModelPickerActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("jarvis_model", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var selectedUri by remember { mutableStateOf(prefs.getString("uri", null)?.let(Uri::parse)) }
            var selectedName by remember { mutableStateOf(selectedUri?.let { displayName(it) } ?: "No model selected") }

            val picker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // Some providers do not offer persistable permissions.
                    }
                    prefs.edit().putString("uri", uri.toString()).putString("name", displayName(uri)).apply()
                    selectedUri = uri
                    selectedName = displayName(uri)
                }
            }

            MaterialTheme(colorScheme = darkColorScheme(primary = ModelCyan, background = ModelBg, surface = ModelPanel)) {
                Column(
                    modifier = Modifier.fillMaxSize().background(ModelBg).padding(22.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("J A R V I S", color = ModelCyan, fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("LOCAL AI MODEL", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(28.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth().border(1.dp, ModelCyan.copy(alpha = .35f), RoundedCornerShape(20.dp)).background(ModelPanel, RoundedCornerShape(20.dp)).padding(18.dp)
                    ) {
                        Text("Gemma GGUF", color = ModelCyan, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(selectedName, color = Color.White, fontSize = 14.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Select your downloaded .gguf file. JARVIS stores only its document URI; the 4–5 GB model stays on your device.", color = ModelMuted, fontSize = 11.sp)
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = { picker.launch(arrayOf("application/octet-stream", "application/x-gguf", "*/*")) },
                                colors = ButtonDefaults.buttonColors(containerColor = ModelCyan),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("SELECT MODEL", color = ModelBg) }
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { startActivity(Intent(this@ModelPickerActivity, MainActivity::class.java)); finish() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D1B2D)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("CONTINUE", color = Color.White) }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Recommended: gemma-3n-E4B-it Q4_K_M", color = ModelMuted, fontSize = 10.sp)
                }
            }
        }
    }

    private fun displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment ?: "Selected GGUF model"
    }
}
