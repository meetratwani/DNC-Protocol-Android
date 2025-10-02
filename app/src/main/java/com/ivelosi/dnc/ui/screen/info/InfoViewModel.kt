package com.ivelosi.dnc.ui.screen.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivelosi.dnc.domain.model.message.FileMessage
import com.ivelosi.dnc.domain.repository.ChatRepository
import com.ivelosi.dnc.domain.repository.ContactRepository
import com.ivelosi.dnc.network.NetworkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InfoViewModel(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    val networkManager: NetworkManager,
    private val Nid: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(InfoViewState())
    val uiState: StateFlow<InfoViewState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            contactRepository.getContactByNidAsFlow(Nid).collect { contact ->
                if(contact?.profile != null) {
                    _uiState.value = contact.profile.let { _uiState.value.copy(profile = it) }
                }
            }
        }

        viewModelScope.launch {
            chatRepository.getAllMediaMessagesByNidAsFlow(Nid).collect { messages ->
                val mediaMessages = messages.filterIsInstance<FileMessage>()
                _uiState.value = _uiState.value.copy(mediaMessages = mediaMessages)
            }
        }
    }
}