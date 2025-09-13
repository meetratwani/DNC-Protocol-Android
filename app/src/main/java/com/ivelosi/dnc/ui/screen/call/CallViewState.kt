package com.ivelosi.dnc.ui.screen.call

import com.ivelosi.dnc.domain.model.device.Account
import com.ivelosi.dnc.domain.model.device.Contact
import com.ivelosi.dnc.domain.model.device.Profile

enum class CallState {
    SENT_CALL_REQUEST, RECEIVED_CALL_REQUEST, CALL
}

data class CallViewState(
    val callState: CallState,
    val contact: Contact = Contact(Account(0, 0), Profile(0, 0, "", null)),
    val isSpeakerOn: Boolean = false
)

