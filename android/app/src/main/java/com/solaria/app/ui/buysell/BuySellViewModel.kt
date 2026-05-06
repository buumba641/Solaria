package com.solaria.app.ui.buysell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.db.*
import com.solaria.app.data.repository.SolariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SwapUiState(
    val fromToken: String = "SOL",
    val toToken: String = "USDC",
    val fromAmount: String = "",
    val toAmount: String = "",
    val solBalance: Double = 0.0,
    val usdcBalance: Double = 0.0,
    val solPrice: Double = 145.0,
    val estimatedFee: Double = 0.000005,
    val isSwapping: Boolean = false,
    val swapSuccess: String? = null,
    val swapError: String? = null,
    val slippagePct: Double = 0.5
)

@HiltViewModel
class BuySellViewModel @Inject constructor(
    private val repo: SolariaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SwapUiState())
    val state: StateFlow<SwapUiState> = _state.asStateFlow()

    init {
        loadBalances()
        loadPrice()
    }

    private fun loadBalances() {
        viewModelScope.launch {
            repo.observeConnectedWallet().collect { wallet ->
                if (wallet != null) {
                    _state.update { it.copy(solBalance = wallet.balanceSol) }
                    // Load USDC balance from token balances
                    repo.observeTokenBalances(wallet.address).collect { tokens ->
                        val usdc = tokens.find { it.symbol == "USDC" }
                        _state.update { it.copy(usdcBalance = usdc?.amount ?: 0.0) }
                    }
                }
            }
        }
    }

    private fun loadPrice() {
        viewModelScope.launch {
            try {
                repo.refreshSolPrice()
            } catch (_: Exception) { /* use cached */ }
            val sol = repo.getPrice("SOL")
            if (sol != null) {
                _state.update { it.copy(solPrice = sol.price) }
            }
        }
    }

    fun updateFromAmount(amount: String) {
        // Only allow valid decimal input
        if (amount.isNotEmpty() && !amount.matches(Regex("""^\d*\.?\d*$"""))) return
        val parsed = amount.toDoubleOrNull() ?: 0.0
        val st = _state.value
        val toAmount = if (st.fromToken == "SOL") {
            parsed * st.solPrice
        } else {
            if (st.solPrice > 0) parsed / st.solPrice else 0.0
        }
        _state.update {
            it.copy(
                fromAmount = amount,
                toAmount = if (parsed > 0) "%.6f".format(toAmount).trimEnd('0').trimEnd('.') else ""
            )
        }
    }

    fun flipTokens() {
        val st = _state.value
        _state.update {
            it.copy(
                fromToken = st.toToken,
                toToken = st.fromToken,
                fromAmount = "",
                toAmount = ""
            )
        }
    }

    fun setSlippage(pct: Double) {
        _state.update { it.copy(slippagePct = pct) }
    }

    fun executeSwap() {
        val st = _state.value
        val fromAmt = st.fromAmount.toDoubleOrNull() ?: return
        if (fromAmt <= 0) return

        // Check sufficient balance
        val available = if (st.fromToken == "SOL") st.solBalance else st.usdcBalance
        if (fromAmt > available) {
            _state.update { it.copy(swapError = "Insufficient ${st.fromToken} balance") }
            return
        }

        _state.update { it.copy(isSwapping = true, swapError = null) }
        viewModelScope.launch {
            try {
                // Simulate swap delay (realistic DEX feel)
                kotlinx.coroutines.delay(1500)

                val toAmt = st.toAmount.toDoubleOrNull() ?: return@launch
                val wallet = repo.observeConnectedWallet().first() ?: return@launch

                if (st.fromToken == "SOL") {
                    // SOL → USDC: decrease SOL, increase USDC
                    repo.updateLocalBalance(
                        walletAddress = wallet.address,
                        newSolBalance = wallet.balanceSol - fromAmt
                    )
                    repo.updateTokenBalance(
                        walletAddress = wallet.address,
                        symbol = "USDC",
                        mint = DEVNET_USDC_MINT,
                        newAmount = st.usdcBalance + toAmt,
                        usdValue = st.usdcBalance + toAmt // 1 USDC = $1
                    )
                } else {
                    // USDC → SOL: decrease USDC, increase SOL
                    repo.updateLocalBalance(
                        walletAddress = wallet.address,
                        newSolBalance = wallet.balanceSol + toAmt
                    )
                    repo.updateTokenBalance(
                        walletAddress = wallet.address,
                        symbol = "USDC",
                        mint = DEVNET_USDC_MINT,
                        newAmount = st.usdcBalance - fromAmt,
                        usdValue = st.usdcBalance - fromAmt
                    )
                }

                // Record the swap transaction
                repo.createTransaction(
                    fromAddress = wallet.address,
                    toAddress = "Jupiter DEX (Simulated)",
                    amountSol = if (st.fromToken == "SOL") fromAmt else toAmt,
                    type = "SWAP",
                    paymentMethod = "DEVNET",
                    description = "Swap ${st.fromAmount} ${st.fromToken} → ${st.toAmount} ${st.toToken}"
                )

                _state.update {
                    it.copy(
                        isSwapping = false,
                        swapSuccess = "Swapped ${st.fromAmount} ${st.fromToken} → ${st.toAmount} ${st.toToken}",
                        fromAmount = "",
                        toAmount = "",
                        solBalance = if (st.fromToken == "SOL") wallet.balanceSol - fromAmt else wallet.balanceSol + toAmt,
                        usdcBalance = if (st.fromToken == "USDC") st.usdcBalance - fromAmt else st.usdcBalance + toAmt
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isSwapping = false, swapError = e.message ?: "Swap failed")
                }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(swapSuccess = null, swapError = null) }
    }

    companion object {
        const val DEVNET_USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
    }
}
