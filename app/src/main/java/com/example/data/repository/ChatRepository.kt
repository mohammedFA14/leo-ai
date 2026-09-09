package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.model.AiModel
import com.example.data.remote.AiApiClient
import com.example.data.remote.dto.DeepSeekMessage
import com.example.data.remote.dto.DeepSeekRequest
import com.example.data.remote.dto.GeminiContent
import com.example.data.remote.dto.GeminiGenerationConfig
import com.example.data.remote.dto.GeminiInlineData
import com.example.data.remote.dto.GeminiPart
import com.example.data.remote.dto.GeminiRequest
import com.example.data.sync.CloudSyncManager
import com.example.util.FileAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val chatDao: ChatDao,
    private val cloudSyncManager: CloudSyncManager
) {
    val allSessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(
        title: String = "محادثة جديدة مع ليو",
        modelUsed: String = AiModel.GEMINI_3_5_FLASH.id
    ): ChatSessionEntity = withContext(Dispatchers.IO) {
        val session = ChatSessionEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false,
            modelUsed = modelUsed
        )
        chatDao.insertSession(session)
        cloudSyncManager.triggerAutoSync()
        session
    }

    suspend fun updateSessionTitle(sessionId: String, title: String) = withContext(Dispatchers.IO) {
        val sessions = chatDao.getAllSessionsList()
        val session = sessions.find { it.id == sessionId } ?: return@withContext
        val updated = session.copy(title = title, updatedAt = System.currentTimeMillis(), isSynced = false)
        chatDao.updateSession(updated)
        cloudSyncManager.triggerAutoSync()
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
        cloudSyncManager.triggerAutoSync()
    }

    suspend fun clearSessionMessages(sessionId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessagesForSession(sessionId)
        cloudSyncManager.triggerAutoSync()
    }

    suspend fun sendMessage(
        sessionId: String,
        prompt: String,
        model: AiModel,
        attachment: FileAnalysisResult? = null,
        customApiKey: String? = null,
        temperature: Float = 0.7f
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val trimmedPrompt = prompt.trim()
        val displayPrompt = if (trimmedPrompt.isNotBlank()) {
            trimmedPrompt
        } else if (attachment != null) {
            if (attachment.isImage) "قم بفحص هذه الصورة وتحليل تفاصيلها بدقة." else "قم بفحص محتوى هذا الملف وتحليله وتلخيصه."
        } else {
            ""
        }

        // 1. Save user message to database with attachment metadata
        val userMessage = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            content = displayPrompt,
            timestamp = System.currentTimeMillis(),
            isSynced = false,
            model = model.id,
            attachmentUri = attachment?.uriString,
            attachmentName = attachment?.fileName,
            attachmentType = if (attachment != null) (if (attachment.isImage) "image" else "document") else null
        )
        chatDao.insertMessage(userMessage)

        // Auto-update session title if it's the first message
        val currentMessages = chatDao.getMessagesForSessionList(sessionId)
        if (currentMessages.size <= 1) {
            val autoTitle = when {
                trimmedPrompt.length > 28 -> trimmedPrompt.take(28) + "..."
                trimmedPrompt.isNotBlank() -> trimmedPrompt
                attachment != null -> "تحليل: ${attachment.fileName}"
                else -> "محادثة ليو AI"
            }
            updateSessionTitle(sessionId, autoTitle)
        }

        // 2. Call the chosen AI model
        try {
            val replyText = when (model) {
                AiModel.GEMINI_3_5_FLASH, AiModel.GEMINI_3_1_PRO -> {
                    callGemini(
                        modelId = model.id,
                        prompt = displayPrompt,
                        attachment = attachment,
                        history = currentMessages,
                        customKey = customApiKey,
                        temperature = temperature
                    )
                }
                AiModel.DEEPSEEK_CHAT -> {
                    callDeepSeek(
                        prompt = displayPrompt,
                        attachment = attachment,
                        history = currentMessages,
                        customKey = customApiKey,
                        temperature = temperature
                    )
                }
            }

            // 3. Save assistant message to database
            val assistantMessage = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "assistant",
                content = replyText,
                timestamp = System.currentTimeMillis(),
                isSynced = false,
                model = model.id
            )
            chatDao.insertMessage(assistantMessage)

            // 4. Update session timestamp
            val session = chatDao.getAllSessionsList().find { it.id == sessionId }
            if (session != null) {
                chatDao.updateSession(session.copy(updatedAt = System.currentTimeMillis(), isSynced = false))
            }

            // 5. Trigger cloud sync
            cloudSyncManager.triggerAutoSync()

            Result.success(assistantMessage)
        } catch (e: Exception) {
            val errorMessage = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "assistant",
                content = "⚠️ واجه 'ليو AI' مشكلة أثناء فحص الملف أو الاتصال:\n${e.localizedMessage ?: "خطأ غير متوقع"}\n\nيرجى التأكد من مفتاح الـ API والاتصال بالإنترنت.",
                timestamp = System.currentTimeMillis(),
                isSynced = false,
                model = model.id
            )
            chatDao.insertMessage(errorMessage)
            Result.failure(e)
        }
    }

    private suspend fun callGemini(
        modelId: String,
        prompt: String,
        attachment: FileAnalysisResult?,
        history: List<ChatMessageEntity>,
        customKey: String?,
        temperature: Float
    ): String {
        val apiKey = if (!customKey.isNullOrBlank()) {
            customKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return "مرحباً بك! أنا **ليو AI** (Leo AI) 🦁✨\n\nأنا جاهز تماماً لفحص ملفاتك وصورك بدقة. للبدء:\n1. تأكد من إضافة مفتاح **GEMINI_API_KEY** في لوحة الأسرار (Secrets Panel) أو الإعدادات ⚙️.\n2. بعد إضافة المفتاح سأتمكن من قراءة الصور والمستندات فوراً!"
        }

        // Build conversational history (keep last 8 turns for efficiency)
        val contents = mutableListOf<GeminiContent>()
        val recentHistory = history.takeLast(8)
        for (msg in recentHistory) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(GeminiContent(role = role, parts = listOf(GeminiPart(text = msg.content))))
        }

        // Build current turn parts with optional image or text document
        val currentParts = mutableListOf<GeminiPart>()
        val effectivePrompt = if (attachment != null && !attachment.isImage && !attachment.textContent.isNullOrBlank()) {
            "📄 ملف مرفق للفحص والتحليل: ${attachment.fileName} (${attachment.formattedSize})\nمحتوى الملف:\n```\n${attachment.textContent}\n```\n\nالسؤال / المطلوب:\n$prompt"
        } else {
            prompt
        }
        currentParts.add(GeminiPart(text = effectivePrompt))

        if (attachment != null && attachment.isImage && attachment.base64Data != null) {
            currentParts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = attachment.mimeType,
                        data = attachment.base64Data
                    )
                )
            )
        }

        contents.add(GeminiContent(role = "user", parts = currentParts))

        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = "أنت 'ليو AI' (Leo AI)، رفيق الذكاء الاصطناعي الذكي، الودود والمتحدث بلباقة وإتقان باللغة العربية والإنجليزية. استخدم التنسيق الأنيق الماركداون (Markdown) والعناوين والنقاط والأكواد البرمجية المرتبة عند الحاجة. قدم إجابات مباشرة ومفيدة وشيقة."
                )
            )
        )

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(temperature = temperature)
        )

        val response = AiApiClient.geminiService.generateGeminiContent(
            model = modelId,
            apiKey = apiKey,
            request = request
        )

        val error = response.error
        if (error != null) {
            throw Exception("خطأ من Gemini (${error.code}): ${error.message}")
        }

        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        return text ?: "لم يتم استلام أي نص من النموذج."
    }

    private suspend fun callDeepSeek(
        prompt: String,
        attachment: FileAnalysisResult?,
        history: List<ChatMessageEntity>,
        customKey: String?,
        temperature: Float
    ): String {
        val apiKey = customKey ?: ""
        if (apiKey.isBlank()) {
            return "لتشغيل نموذج **DeepSeek AI (مفتوح المصدر)** 🚀:\nيرجى إدخال مفتاح الـ API الخاص بـ DeepSeek من خلال نافذة الإعدادات ⚙️ في أعلى الشاشة.\n\nأو يمكنك التبديل إلى **Leo Fast (Gemini)** المفعّل مسبقاً!"
        }

        val messages = mutableListOf<DeepSeekMessage>()
        messages.add(
            DeepSeekMessage(
                role = "system",
                content = "أنت 'ليو AI' المعتمد على تقنيات الذكاء الاصطناعي المفتوحة، مساعد ذكي ومتمكن يقدم ردوداً واضحة باللغة العربية ويقوم بفحص وتحليل الملفات والمستندات بدقة."
            )
        )
        val recent = history.takeLast(8)
        for (msg in recent) {
            val role = if (msg.role == "user") "user" else "assistant"
            messages.add(DeepSeekMessage(role = role, content = msg.content))
        }

        val effectivePrompt = if (attachment != null && !attachment.isImage && !attachment.textContent.isNullOrBlank()) {
            "📄 ملف مرفق للفحص: ${attachment.fileName} (${attachment.formattedSize})\n```\n${attachment.textContent}\n```\n\nالسؤال: $prompt"
        } else {
            prompt
        }
        messages.add(DeepSeekMessage(role = "user", content = effectivePrompt))

        val request = DeepSeekRequest(
            model = "deepseek-chat",
            messages = messages,
            temperature = temperature
        )

        val response = AiApiClient.deepSeekService.generateDeepSeekContent(
            authHeader = "Bearer $apiKey",
            request = request
        )

        if (response.error != null) {
            throw Exception("خطأ من DeepSeek: ${response.error.message}")
        }

        return response.choices?.firstOrNull()?.message?.content ?: "لا يوجد رد من خادم DeepSeek."
    }
}
