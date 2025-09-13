package com.ivelosi.dnc.ui.screen.info

import com.ivelosi.dnc.domain.model.device.Profile
import com.ivelosi.dnc.domain.model.message.FileMessage

data class InfoViewState(
    val profile: Profile = Profile(0, 0, "username", null),
    val mediaMessages: List<FileMessage> = listOf()
)
