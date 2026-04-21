package com.example.dwas.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dwas.data.model.User
import com.example.dwas.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    fun loginWithGoogle(credential: AuthCredential) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = repository.signInWithCredential(credential)
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val user = repository.getUser(firebaseUser.uid)
                    if (user != null) {
                        _authState.value = AuthState.Success(role = user.role)
                    } else {
                        // For new Google users, default to employee or handle registration
                        val names = (firebaseUser.displayName ?: "New User").split(" ")
                        val firstName = names.getOrNull(0) ?: "New"
                        val lastName = names.getOrNull(1) ?: "User"
                        val newUser = User(
                            uid = firebaseUser.uid,
                            firstName = firstName,
                            lastName = lastName,
                            email = firebaseUser.email ?: "",
                            role = "employee" // Default role
                        )
                        repository.saveUser(newUser)
                        _authState.value = AuthState.Success(role = newUser.role)
                    }
                } else {
                    _authState.value = AuthState.Error("Google Sign-In failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "An error occurred during Google Sign-In")
            }
        }
    }

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

    fun resetPassword(email: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repository.getUserByEmail(email)
                if (user != null) {
                    repository.sendPasswordResetEmail(email)
                    if (user.supervisorId != null) {
                        val supervisorEmail = repository.getSupervisorEmail(user.supervisorId)
                        _authState.value = AuthState.Success("Reset link sent. Supervisor ($supervisorEmail) notified for verification.")
                    } else {
                        _authState.value = AuthState.Success("Reset link sent.")
                    }
                } else {
                    _authState.value = AuthState.Error("User with this email not found")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Reset failed")
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