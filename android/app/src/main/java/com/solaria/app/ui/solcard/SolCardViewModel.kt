package com.solaria.app.ui.solcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.auth.FirebaseAuthManager
import com.solaria.app.data.solcard.SimulatedCardManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SolCardUiState(
    val cardNumber: String = "**** **** **** ****",
    val expiry: String = "--/--",
    val cvv: String = "***",
    val cardholder: String = "",
    val balance: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SolCardViewModel @Inject constructor(
    private val cardManager: SimulatedCardManager,
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SolCardUiState())
    val uiState: StateFlow<SolCardUiState> = _uiState.asStateFlow()

    init {
        loadCard()
    }

    fun loadCard() {
        val userId = authManager.getCurrentUser() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val details = cardManager.getCardDetails(userId)
                if (details != null) {
                    _uiState.value = SolCardUiState(
                        cardNumber = details["cardNumber"] as String,
                        expiry = details["expiry"] as String,
                        cvv = details["cvv"] as String,
                        cardholder = details["cardholder"] as String,
                        balance = details["balance"] as Double,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
