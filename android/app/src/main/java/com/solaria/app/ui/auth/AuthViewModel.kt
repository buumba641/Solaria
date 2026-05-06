package com.solaria.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.auth.FirebaseAuthManager
import com.solaria.app.data.firestore.UserDataManager
import com.solaria.app.data.solana.DemoWalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userId: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val walletManager: DemoWalletManager,
    private val userDataManager: UserDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val userId = authManager.signUp(email, password)
                if (userId != null) {
                    // Initialize profile
                    userDataManager.saveProfile(userId, mapOf(
                        "email" to email,
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    ))
                    // Generate wallet
                    walletManager.generateAndStoreWallet(userId)
                    _uiState.value = AuthUiState.Success(userId)
                } else {
                    _uiState.value = AuthUiState.Error("Sign up failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val userId = authManager.signIn(email, password)
                if (userId != null) {
                    // Load wallet from Firestore
                    walletManager.loadWalletFromFirestore(userId)
                    _uiState.value = AuthUiState.Success(userId)
                } else {
                    _uiState.value = AuthUiState.Error("Sign in failed")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    /** Check if a user is currently authenticated. Used by AppNavigation. */
    fun isLoggedIn(): Boolean = authManager.getCurrentUser() != null
}
