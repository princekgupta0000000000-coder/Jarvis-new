package com.nexus.jarvis

import android.content.Context
import android.net.Uri
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline Gemma/GGUF bridge. The model is selected from shared storage, copied once
 * into app-private storage, then executed locally through the llama.cpp Android binding.
 */
object JarvisAiEngine {
    private var engine: InferenceEngine? = null
    private var loadedPath: String? = null

    suspend fun ask(
        context: Context,
        prompt: String,
        onToken: (String) -> Unit
    ): Result<Unit> = runCatching {
        val inference = ensureLoaded(context)
        inference.sendUserPrompt(prompt, predictLength = 512).collect(onToken)
    }

    suspend fun ensureLoaded(context: Context): InferenceEngine = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("jarvis_model", Context.MODE_PRIVATE)
        val uriString = prefs.getString("uri", null)
            ?: error("No GGUF model selected. Open Model Setup and select your Gemma GGUF file.")
        val uri = Uri.parse(uriString)
        val name = (prefs.getString("name", null) ?: "jarvis-model.gguf")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val target = File(modelsDir, name)

        val sourceLength = contentLength(context, uri)
        if (!target.exists() || (sourceLength > 0 && target.length() != sourceLength)) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output, DEFAULT_BUFFER) }
            } ?: error("JARVIS cannot read the selected GGUF file.")
        }

        val current = engine
        if (current != null && loadedPath == target.absolutePath && current.state.value is InferenceEngine.State.ModelReady) {
            return@withContext current
        }

        val instance = current ?: AiChat.getInferenceEngine(context.applicationContext).also { engine = it }
        if (instance.state.value is InferenceEngine.State.Uninitialized || instance.state.value is InferenceEngine.State.Initializing) {
            instance.state.filter { it is InferenceEngine.State.Initialized || it is InferenceEngine.State.Error }.first().let {
                if (it is InferenceEngine.State.Error) error(it.exception.message ?: "Local AI runtime failed to initialize")
            }
        }
        if (loadedPath != target.absolutePath || instance.state.value !is InferenceEngine.State.ModelReady) {
            if (instance.state.value is InferenceEngine.State.ModelReady) instance.cleanUp()
            instance.loadModel(target.absolutePath)
            instance.setSystemPrompt(SYSTEM_PROMPT)
            loadedPath = target.absolutePath
        }
        instance
    }

    private fun contentLength(context: Context, uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else -1L
        } ?: -1L
    }.getOrDefault(-1L)

    private const val DEFAULT_BUFFER = 1024 * 1024

    private const val SYSTEM_PROMPT = """
You are JARVIS, a concise personal Android AI assistant for Prince.
You run locally on the user's phone. Be helpful, natural, confident and honest.
Use Indian English/Hinglish naturally when the user speaks Hinglish.
You know the user's SIT Sitamarhi CSE (AI & ML) Semester 1 timetable, G2 lab schedule,
attendance workflow, study tasks and holidays when the app supplies that context.
Do not claim you performed an Android action unless the app actually did it.
For normal questions, answer directly. For calculations, show the key result briefly.
Do not invent personal facts, timetable entries or current information.
""".trimIndent()
}
