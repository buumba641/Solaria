package com.solaria.app.ui.bitrefill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.auth.FirebaseAuthManager
import com.solaria.app.data.bitrefill.BitrefillService
import com.solaria.app.data.firestore.UserDataManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BitrefillUiState {
    object Idle : BitrefillUiState()
    object Processing : BitrefillUiState()
    data class Success(val code: String) : BitrefillUiState()
    data class Error(val message: String) : BitrefillUiState()
}

@HiltViewModel
class BitrefillViewModel @Inject constructor(
    private val bitrefillService: BitrefillService,
    private val authManager: FirebaseAuthManager,
    private val userDataManager: UserDataManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<BitrefillUiState>(BitrefillUiState.Idle)
    val uiState: StateFlow<BitrefillUiState> = _uiState.asStateFlow()

    fun purchaseGiftCard(operator: String, amount: Double) {
        val userId = authManager.getCurrentUser() ?: return
        viewModelScope.launch {
            _uiState.value = BitrefillUiState.Processing
            try {
                val invoiceId = bitrefillService.createInvoice(operator, amount)
                if (invoiceId != null) {
                    // Poll for success in demo
                    delay(3000)
                    val code = "REDEEM-SOL-123-ABC" // Simulated code
                    
                    userDataManager.addBitrefillOrder(userId, mapOf(
                        "invoiceId" to invoiceId,
                        "operator" to operator,
                        "amount" to amount,
                        "code" to code,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    
                    _uiState.value = BitrefillUiState.Success(code)
                } else {
                    _uiState.value = BitrefillUiState.Error("Failed to create invoice")
                }
            } catch (e: Exception) {
                _uiState.value = BitrefillUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
