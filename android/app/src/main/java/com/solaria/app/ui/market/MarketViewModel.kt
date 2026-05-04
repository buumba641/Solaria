package com.solaria.app.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solaria.app.data.db.*
import com.solaria.app.data.repository.SolariaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val isLoading: Boolean = false,
    val tokenData: PriceCacheEntity? = null,
    val nftCollections: List<NftCollectionEntity> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val lastPriceSync: String = "Never",
    val lastNftSync: String = "Never"
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val repo: SolariaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        loadToken("SOL")
        loadTopNfts()
        observeSyncStatus()
    }

    fun onSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun searchToken(symbolOrMint: String) {
        if (symbolOrMint.isBlank()) return
        loadToken(symbolOrMint.trim().uppercase())
    }

    private fun loadToken(symbol: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val data = repo.getPrice(symbol)
                if (data != null) {
                    _state.update { it.copy(
                        isLoading = false, tokenData = data,
                        lastPriceSync = repo.formatSyncAge(data.updatedAtMs)
                    )}
                } else {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "No cached data for $symbol. Connect to update."
                    )}
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadTopNfts() {
        viewModelScope.launch {
            repo.observeNftCollections().collect { nfts ->
                val syncAge = nfts.firstOrNull()?.let { repo.formatSyncAge(it.lastSyncedAt) } ?: "Never"
                _state.update { it.copy(nftCollections = nfts, lastNftSync = syncAge) }
            }
        }
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            repo.observeSyncForType("prices").collect { meta ->
                if (meta != null) {
                    _state.update { it.copy(lastPriceSync = repo.formatSyncAge(meta.lastSyncedAt)) }
                }
            }
        }
    }

    fun refresh() {
        loadToken(_state.value.tokenData?.symbol ?: "SOL")
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
}
