package com.example.dwas.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableCornerSize
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dwas.data.model.User
import com.example.dwas.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _userRole = MutableLiveData<String?>()
    val userRole: LiveData<String?> = _userRole

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = repository.login(email, password)
                if (result.user != null) {
                    fetchUserRole(result.user!!.uid)
                } else {
                    _authState.value = AuthState.Error("Login failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun signUp(user: User, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = repository.signUp(user.email, password)
                val uid = result.user?.uid
                if (uid != null) {
                    val userWithUid = user.copy(uid = uid)
                    repository.saveUser(userWithUid)
                    _authState.value = AuthState.Success(role = user.role)
                } else {
                    _authState.value = AuthState.Error("Signup failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun fetchUserRole(uid: String) {
        viewModelScope.launch {
            try {
                val user = repository.getUser(uid)
                if (user != null) {
                    _authState.value = AuthState.Success(role = user.role)
                } else {
                    _authState.value = AuthState.Error("User data not found")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error fetching user role")
            }
        }
    }

    sealed class AuthState {
        object Loading : AuthState()
        data class Success(val role: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}