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
    val pendingSyncCount: Int = 0,
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
                _state.update { it.copy(
                    transactions = txs,
                    pendingSyncCount = txs.count { tx -> tx.status == "PENDING_SYNC" }
                )}
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
        paymentMethod: String, description: String?,
        isOffline: Boolean = false
    ) {
        val wallet = _state.value.wallet ?: return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            try {
                val tx = repo.createTransaction(
                    fromAddress = wallet.address, toAddress = toAddress,
                    amountSol = amountSol, type = if (isOffline) "P2P_SEND" else "SEND",
                    paymentMethod = paymentMethod, description = description,
                    isOffline = isOffline
                )
                // For demo: auto-confirm non-offline transactions after a delay
                if (!isOffline) {
                    kotlinx.coroutines.delay(1500)
                    repo.confirmTransaction(tx.id, "mock_tx_${tx.id.take(8)}")
                }
                _state.update { it.copy(
                    isProcessing = false,
                    successMessage = if (isOffline)
                        "P2P transaction queued. Will sync when online."
                    else
                        "Transaction confirmed!"
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun createP2PTransaction(toAddress: String, amountSol: Double, description: String?) {
        sendTransaction(toAddress, amountSol, "P2P", description, isOffline = true)
    }

    fun dismissMessage() {
        _state.update { it.copy(successMessage = null, error = null) }
    }
}
