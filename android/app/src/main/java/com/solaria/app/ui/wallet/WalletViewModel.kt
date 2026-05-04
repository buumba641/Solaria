package com.solaria.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.db.*
import com.solaria.app.data.repository.SolariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletUiState(
    val walletAddress: String = "",
    val isConnected: Boolean = false,
    val balanceSol: Double = 0.0,
    val tokens: List<TokenBalanceEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val pinEnabled: Boolean = true,
    val lastSyncedAt: String = "Never"
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repo: SolariaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeConnectedWallet().collect { wallet ->
                if (wallet != null) {
                    _state.update { it.copy(
                        walletAddress = wallet.address,
                        isConnected = wallet.isConnected,
                        balanceSol = wallet.balanceSol,
                        lastSyncedAt = repo.formatSyncAge(wallet.lastSyncedAt)
                    )}
                    // Load token balances
                    repo.observeTokenBalances(wallet.address).collect { tokens ->
                        _state.update { it.copy(tokens = tokens) }
                    }
                } else {
                    _state.update { WalletUiState() }
                }
            }
        }
    }

    fun onWalletConnected(address: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                repo.connectWallet(address)
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onWalletDisconnected() {
        viewModelScope.launch { repo.disconnectWallet() }
    }

    fun togglePin(enabled: Boolean) {
        _state.update { it.copy(pinEnabled = enabled) }
    }

    fun refreshBalance() {
        // In offline mode, data is already in DB.
        // When backend is connected, this would trigger a sync.
        _state.update { it.copy(lastSyncedAt = "Just now") }
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
}
