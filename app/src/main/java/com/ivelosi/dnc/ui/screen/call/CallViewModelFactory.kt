package com.ivelosi.dnc.ui.screen.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class CallViewModelFactory(private val accountId: Long, private val callState: CallState) : ViewModelProvider.Factory {
    override fun <T: ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(CallViewModel::class.java)) {
            val application = (extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as com.ivelosi.dnc.App)

            return CallViewModel(
                application.container.contactRepository,
                application.container.callManager,
                application.container.networkManager,
                accountId,
                callState
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}