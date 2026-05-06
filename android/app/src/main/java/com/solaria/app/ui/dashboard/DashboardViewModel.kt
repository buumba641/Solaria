package com.solaria.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.db.*
import com.solaria.app.data.repository.SolariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val wallet: WalletEntity? = null,
    val portfolioValueUsd: Double = 0.0,
    val solPrice: PriceCacheEntity? = null,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val pendingSyncCount: Int = 0, // Reserved for future use
    val syncMetadata: List<SyncMetadataEntity> = emptyList(),
    val lastWalletSync: String = "Never"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: SolariaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        // Observe connected wallet
        viewModelScope.launch {
            repo.observeConnectedWallet().collect { wallet ->
                _state.update { it.copy(
                    wallet = wallet,
                    lastWalletSync = if (wallet != null) repo.formatSyncAge(wallet.lastSyncedAt) else "Never"
                )}
                // Compute portfolio value
                val value = repo.computePortfolioValue()
                _state.update { it.copy(portfolioValueUsd = value) }

                // Auto-refresh balance from DevNet if wallet is connected
                if (wallet != null) {
                    try {
                        repo.refreshBalance(wallet.address)
                        val updatedValue = repo.computePortfolioValue()
                        _state.update { it.copy(portfolioValueUsd = updatedValue) }
                    } catch (_: Exception) {
                        // Offline — use cached data
                    }
                }
            }
        }
        // Observe SOL price and refresh from CoinGecko
        viewModelScope.launch {
            repo.observeSyncForType("prices").collect { meta ->
                val sol = repo.getPrice("SOL")
                _state.update { it.copy(solPrice = sol) }
            }
        }
        // Refresh SOL price from CoinGecko on launch
        viewModelScope.launch {
            try {
                repo.refreshSolPrice()
                val sol = repo.getPrice("SOL")
                _state.update { it.copy(solPrice = sol) }
                // Recompute portfolio with updated price
                val value = repo.computePortfolioValue()
                _state.update { it.copy(portfolioValueUsd = value) }
            } catch (_: Exception) {
                // Offline — use cached price
            }
        }
        // Observe recent transactions
        viewModelScope.launch {
            repo.observeRecentTransactions(5).collect { txs ->
                _state.update { it.copy(
                    recentTransactions = txs
                )}
            }
        }
        // Observe sync metadata
        viewModelScope.launch {
            repo.observeAllSyncMetadata().collect { meta ->
                _state.update { it.copy(syncMetadata = meta) }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            repo.refreshSolPrice()
            _state.value.wallet?.let { repo.refreshBalance(it.address) }
        }
    }
}
