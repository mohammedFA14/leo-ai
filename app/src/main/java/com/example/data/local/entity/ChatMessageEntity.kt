package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val model: String = "gemini-3.5-flash",
    val attachmentUri: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null
)
