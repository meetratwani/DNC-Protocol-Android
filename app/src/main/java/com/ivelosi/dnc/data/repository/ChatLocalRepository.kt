package com.ivelosi.dnc.data.repository

import com.ivelosi.dnc.data.local.account.AccountDAO
import com.ivelosi.dnc.data.local.message.MessageDAO
import com.ivelosi.dnc.data.local.message.MessageEntity
import com.ivelosi.dnc.data.local.profile.ProfileDAO
import com.ivelosi.dnc.domain.model.chat.ChatPreview
import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.device.toAccount
import com.ivelosi.dnc.domain.model.device.toProfile
import com.ivelosi.dnc.domain.model.message.Message
import com.ivelosi.dnc.domain.model.message.MessageState
import com.ivelosi.dnc.domain.model.message.toMessage
import com.ivelosi.dnc.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ChatLocalRepository(private val accountDAO: AccountDAO, private val messageDAO: MessageDAO, private val profileDAO: ProfileDAO) : ChatRepository {
    override fun getAllChatPreviewsAsFlow(): Flow<List<ChatPreview>> {
        val accountsFlow = accountDAO.getAllAccountsAsFlow().map { accountEntities ->
            accountEntities.map { it.toAccount() }
        }

        val profilesFlow = profileDAO.getAllProfilesAsFlow().map { profileEntities ->
            profileEntities.map { it.toProfile() }
        }

        return combine(accountsFlow, profilesFlow) { accounts, profiles ->
            accounts.map { account ->
                ChatPreview(
                    Contact(account, profiles.find { it.Nid == account.Nid }),
                    messageDAO.getCountOfUnreadMessagesByNid(account.Nid).toInt(),
                    messageDAO.getLastMessageByNid(account.Nid)?.toMessage()
                )
            }.sorted().reversed()
        }
    }

    override fun getAllMessagesByNidAsFlow(Nid: Long): Flow<List<Message>> {
        return messageDAO.getAllMessagesByNid(Nid).map { messageEntities ->
            messageEntities.map { it.toMessage() }
        }
    }

    override fun getAllMediaMessagesByNidAsFlow(Nid: Long): Flow<List<Message>> {
        return messageDAO.getAllMediaMessagesByNid(Nid).map { messageEntities ->
            messageEntities.map { it.toMessage() }
        }
    }

    override fun getAllMessagesByReceiverNid(Nid: Long): List<Message> {
        return messageDAO.getMessagesByReceiverNid(Nid).map { it.toMessage() }
    }

    override suspend fun getAllMessages(): List<MessageEntity> {
        return messageDAO.getAllMessages()
    }

    override suspend fun getMessageByMessageId(messageId: Long): Message? {
        return messageDAO.getMessageByMessageId(messageId)?.toMessage()
    }

    override suspend fun addMessage(message: Message): Long {
        return messageDAO.insertMessage(message.toMessageEntity())
    }

    override suspend fun updateMessage(message: Message) {
        messageDAO.updateMessage(message.toMessageEntity())
    }

    override suspend fun updateMessageState(messageId: Long, messageState: MessageState) {
        messageDAO.updateMessageState(messageId, messageState)
    }
}