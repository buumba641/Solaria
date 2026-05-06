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
    val isAirdropping: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val pinEnabled: Boolean = true,
    val lastSyncedAt: String = "Never",
    val hasLocalKeypair: Boolean = false,
    val txSignature: String? = null
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
                    val hasKeypair = repo.keypairManager.hasKeypair(wallet.address)
                    _state.update { it.copy(
                        walletAddress = wallet.address,
                        isConnected = wallet.isConnected,
                        balanceSol = wallet.balanceSol,
                        lastSyncedAt = repo.formatSyncAge(wallet.lastSyncedAt),
                        hasLocalKeypair = hasKeypair
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

    /**
     * Generate a new Solana keypair on-device.
     * Equivalent to `solana-keygen new`.
     */
    fun generateNewWallet() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val address = repo.generateWallet()
                _state.update { it.copy(
                    isLoading = false,
                    successMessage = "Wallet generated! Address: ${address.take(8)}…"
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Failed to generate wallet: ${e.message}") }
            }
        }
    }

    /**
     * Connect an existing wallet by address (watch-only, no signing).
     */
    fun onWalletConnected(address: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                repo.connectWallet(address)
                _state.update { it.copy(isLoading = false) }
                // Try to fetch balance from DevNet
                refreshBalance()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Import a wallet from a private key.
     */
    fun importWallet(privateKeyBase58: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val address = repo.keypairManager.importFromPrivateKey(privateKeyBase58)
                repo.connectWallet(address)
                _state.update { it.copy(
                    isLoading = false,
                    successMessage = "Wallet imported! Address: ${address.take(8)}…"
                )}
                refreshBalance()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Import failed: ${e.message}") }
            }
        }
    }

    fun onWalletDisconnected() {
        viewModelScope.launch { repo.disconnectWallet() }
    }

    /**
     * Fetch real balance from Solana DevNet.
     * Equivalent to `solana balance`.
     */
    fun refreshBalance() {
        val address = _state.value.walletAddress
        if (address.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val sol = repo.refreshBalance(address)
                _state.update { it.copy(
                    isLoading = false,
                    balanceSol = sol,
                    lastSyncedAt = "Just now"
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Balance fetch failed: ${e.message}"
                )}
            }
        }
    }

    /**
     * Request DevNet SOL airdrop.
     * Equivalent to `solana airdrop 1`.
     */
    fun requestAirdrop(amountSol: Double = 1.0) {
        val address = _state.value.walletAddress
        if (address.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isAirdropping = true, error = null, successMessage = null) }
            try {
                val sig = repo.requestAirdrop(address, amountSol)
                _state.update { it.copy(
                    isAirdropping = false,
                    successMessage = "Airdrop of ${"%.1f".format(amountSol)} SOL received! ✅",
                    txSignature = sig
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isAirdropping = false,
                    error = "Airdrop failed: ${e.message}"
                )}
            }
        }
    }

    /**
     * Send SOL to another address on DevNet.
     * Builds, signs, and broadcasts a real transaction.
     */
    fun sendSol(toAddress: String, amountSol: Double) {
        val fromAddress = _state.value.walletAddress
        if (fromAddress.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null, successMessage = null) }
            try {
                val sig = repo.sendSol(fromAddress, toAddress, amountSol)
                _state.update { it.copy(
                    isSending = false,
                    successMessage = "Sent ${"%.4f".format(amountSol)} SOL! TX: ${sig.take(12)}…",
                    txSignature = sig
                )}
            } catch (e: Exception) {
                _state.update { it.copy(
                    isSending = false,
                    error = "Transfer failed: ${e.message}"
                )}
            }
        }
    }

    fun togglePin(enabled: Boolean) {
        _state.update { it.copy(pinEnabled = enabled) }
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
    fun dismissSuccess() { _state.update { it.copy(successMessage = null, txSignature = null) } }
}
