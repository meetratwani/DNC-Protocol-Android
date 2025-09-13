package com.ivelosi.dnc.ui.screen.chat

import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.device.Profile
import com.ivelosi.dnc.domain.model.message.Message


data class ChatViewState(
    val contact: Contact = Contact(Account(0, 0), Profile(0, 0, "", null)),
    val messages: List<Message> = listOf()
)