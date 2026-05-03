package com.solaria.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.api.SolariaApiService
import com.solaria.app.data.models.BalanceResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletUiState(
    val walletAddress: String = "",
    val isConnected: Boolean = false,
    val balance: BalanceResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pinEnabled: Boolean = true
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val api: SolariaApiService
) : ViewModel() {

    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    fun onWalletConnected(address: String) {
        _state.update { it.copy(walletAddress = address, isConnected = true) }
        fetchBalance(address)
    }

    fun onWalletDisconnected() {
        _state.update { WalletUiState() }
    }

    fun togglePin(enabled: Boolean) {
        _state.update { it.copy(pinEnabled = enabled) }
    }

    fun refreshBalance() {
        val addr = _state.value.walletAddress
        if (addr.isNotBlank()) fetchBalance(addr)
    }

    private fun fetchBalance(address: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val balance = api.getBalance(address)
                _state.update { it.copy(isLoading = false, balance = balance) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
}
