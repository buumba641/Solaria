package com.solaria.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted chat message for local history.
 * Mirrors the mifos-pay "transaction detail" card concept but
 * adapted for a conversational bot thread.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,           // "user" | "bot"
    val text: String,
    val audioUrl: String? = null,
    val actionType: String? = null,   // TRANSFER | CROSS_CHAIN | null
    val actionPayloadJson: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)
