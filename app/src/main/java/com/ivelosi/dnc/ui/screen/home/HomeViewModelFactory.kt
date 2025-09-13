package com.ivelosi.dnc.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T: ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val application = (extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as com.ivelosi.dnc.App)

            return HomeViewModel(
                application.container.chatRepository,
                application.container.networkManager
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}