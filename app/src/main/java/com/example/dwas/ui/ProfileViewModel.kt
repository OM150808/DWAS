package com.example.dwas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dwas.data.model.User
import com.example.dwas.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    object Empty : ProfileUiState()
}

class ProfileViewModel(private val repository: UserRepository = UserRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        fetchUserProfile()
    }

    private fun fetchUserProfile() {
        val userId = repository.getCurrentUserId()
        if (userId == null) {
            _uiState.value = ProfileUiState.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            repository.getUserProfileRealtime(userId).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        if (user != null) {
                            _uiState.value = ProfileUiState.Success(user)
                        } else {
                            _uiState.value = ProfileUiState.Empty
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = ProfileUiState.Error(error.message ?: "Unknown error occurred")
                    }
                )
            }
        }
    }

    fun logout() {
        repository.logout()
    }
}