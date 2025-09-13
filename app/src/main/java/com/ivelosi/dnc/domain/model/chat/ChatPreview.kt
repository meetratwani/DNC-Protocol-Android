package com.ivelosi.dnc.domain.model.chat

import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.message.Message

data class ChatPreview(
    val contact: Contact,
    val unreadMessagesCount: Int,
    val lastMessage: Message?
) : Comparable<ChatPreview> {
    override fun compareTo(other: ChatPreview): Int {
        return compareValuesBy(this, other, ChatPreview::lastMessage, ChatPreview::contact, ChatPreview::unreadMessagesCount)
    }
}