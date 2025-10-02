package com.ivelosi.dnc.data.local.message

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ivelosi.dnc.domain.model.message.MessageState
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDAO {
    @Query("SELECT * FROM MessageEntity WHERE (senderId = :Nid OR receiverId = :Nid) ORDER BY timestamp ASC")
    fun getAllMessagesByNid(Nid: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageEntity WHERE receiverId = :Nid ORDER BY timestamp ASC")
    fun getMessagesByReceiverNid(Nid: Long): List<MessageEntity>

    @Query("SELECT * FROM MessageEntity WHERE (senderId = :Nid OR receiverId = :Nid) AND messageType = 'FILE_MESSAGE' ORDER BY timestamp ASC")
    fun getAllMediaMessagesByNid(Nid: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM MessageEntity")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM MessageEntity WHERE senderId = :Nid AND messageState = 'MESSAGE_RECEIVED'")
    suspend fun getCountOfUnreadMessagesByNid(Nid: Long): Long

    @Query("SELECT * FROM MessageEntity WHERE (senderId = :Nid OR receiverId = :Nid) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageByNid(Nid: Long): MessageEntity?

    @Query("SELECT * FROM MessageEntity WHERE messageId = :messageId")
    suspend fun getMessageByMessageId(messageId: Long): MessageEntity?

    @Insert
    suspend fun insertMessage(messageEntity: MessageEntity): Long

    @Update
    suspend fun updateMessage(messageEntity: MessageEntity)

    @Query("UPDATE MessageEntity SET messageState = :messageState WHERE messageId = :messageId")
    suspend fun updateMessageState(messageId: Long, messageState: MessageState)
}