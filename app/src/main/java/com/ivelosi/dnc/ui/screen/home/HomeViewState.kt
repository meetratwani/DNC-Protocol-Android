package com.ivelosi.dnc.ui.screen.home

import com.ivelosi.dnc.domain.model.chat.ChatPreview

data class HomeViewState(
    val chatPreviews: List<ChatPreview> = listOf(),
    val onlineChats: Set<Long> = setOf()
)
