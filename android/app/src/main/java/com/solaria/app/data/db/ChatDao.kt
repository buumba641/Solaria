package com.solaria.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :convId ORDER BY timestampMs ASC")
    fun observeMessages(convId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversationId = :convId")
    suspend fun clearConversation(convId: String)

    @Query("SELECT * FROM chat_messages ORDER BY timestampMs DESC LIMIT 50")
    suspend fun getRecentMessages(): List<ChatMessageEntity>
}
