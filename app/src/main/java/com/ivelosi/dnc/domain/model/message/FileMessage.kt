package com.ivelosi.dnc.domain.model.message

import com.ivelosi.dnc.data.local.message.MessageEntity
import com.ivelosi.dnc.network.model.message.NetworkFileMessage

data class FileMessage(
    override val messageId: Long,
    override val senderId: Long,
    override val receiverId: Long,
    override val timestamp: Long,
    override val messageState: MessageState,
    val fileName: String
) : Message() {
    constructor(networkFileMessage: NetworkFileMessage, messageState: MessageState) : this(
        messageId = networkFileMessage.messageId,
        senderId = networkFileMessage.senderId,
        receiverId = networkFileMessage.receiverId,
        timestamp = networkFileMessage.timestamp,
        messageState = messageState,
        fileName = networkFileMessage.fileName
    )

    override fun toMessageEntity(): MessageEntity {
        return MessageEntity(
            senderId = senderId,
            receiverId = receiverId,
            timestamp = timestamp,
            messageState = messageState,
            messageType = MessageType.FILE_MESSAGE,
            content = fileName
        )
    }
}

fun MessageEntity.toFileMessage(): FileMessage {
    return FileMessage(
        messageId = messageId,
        senderId = senderId,
        receiverId = receiverId,
        timestamp = timestamp,
        messageState = messageState,
        fileName = content
    )
}