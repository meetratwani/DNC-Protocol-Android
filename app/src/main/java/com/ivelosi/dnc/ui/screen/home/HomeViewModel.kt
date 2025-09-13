package com.ivelosi.dnc.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivelosi.dnc.domain.repository.ChatRepository
import com.ivelosi.dnc.network.NetworkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val chatRepository: ChatRepository,
    val networkManager: NetworkManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeViewState())
    val uiState: StateFlow<HomeViewState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getAllChatPreviewsAsFlow().collect {
                _uiState.value = _uiState.value.copy(chatPreviews = it)
            }
        }

        viewModelScope.launch {
            networkManager.connectedDevices.collect { device ->
                _uiState.value = _uiState.value.copy(onlineChats = device.map { it.account.accountId }.toSet())
            }
        }
    }

    fun connect() {
        networkManager.receiver.discoverPeers()
    }
}
