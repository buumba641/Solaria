package com.solaria.app.ui.payments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.db.*
import com.solaria.app.data.repository.SolariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val paymentMethods: List<PaymentMethodEntity> = emptyList(),
    val wallet: WalletEntity? = null,
    val isProcessing: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repo: SolariaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentsUiState())
    val state: StateFlow<PaymentsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAllTransactions().collect { txs ->
                _state.update { it.copy(transactions = txs) }
            }
        }
        viewModelScope.launch {
            repo.observePaymentMethods().collect { methods ->
                _state.update { it.copy(paymentMethods = methods) }
            }
        }
        viewModelScope.launch {
            repo.observeConnectedWallet().collect { wallet ->
                _state.update { it.copy(wallet = wallet) }
            }
        }
    }

    fun sendTransaction(
        toAddress: String, amountSol: Double,
        paymentMethod: String, description: String?
    ) {
        val wallet = _state.value.wallet ?: return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            try {
                val hasKeypair = repo.keypairManager.hasKeypair(wallet.address)

                if (hasKeypair && paymentMethod == "DEVNET") {
                    // REAL TRANSACTION: sign and broadcast on DevNet
                    val txSig = repo.sendSol(wallet.address, toAddress, amountSol)
                    _state.update { it.copy(
                        isProcessing = false,
                        successMessage = "✅ Transaction confirmed on DevNet! TX: ${txSig.take(12)}…"
                    )}
                } else {
                    // Watch-only wallet — queue for later signing
                    val tx = repo.createTransaction(
                        fromAddress = wallet.address, toAddress = toAddress,
                        amountSol = amountSol, type = "SEND",
                        paymentMethod = paymentMethod, description = description
                    )
                    _state.update { it.copy(
                        isProcessing = false,
                        successMessage = if (!hasKeypair)
                            "Transaction queued (watch-only wallet). Import private key to broadcast."
                        else
                            "Transaction queued."
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(successMessage = null, error = null) }
    }
}
