package com.solaria.app.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.solaria.app.data.api.SolariaApiService
import com.solaria.app.data.db.PriceCacheDao
import com.solaria.app.data.db.PriceCacheEntity
import com.solaria.app.data.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MarketUiState(
    val isLoading: Boolean = false,
    val tokenData: TokenPerformanceResponse? = null,
    val nftCollections: List<NftCollection> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val api: SolariaApiService,
    private val cacheDao: PriceCacheDao
) : ViewModel() {

    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    private val gson = Gson()

    init {
        loadTopNfts()
        loadToken("SOL")  // default token on open
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

            // Check cache first (< 5 min)
            val cached = cacheDao.getBySymbol(symbol)
            if (cached != null && System.currentTimeMillis() - cached.updatedAtMs < 300_000) {
                val history = cached.historyJson?.let {
                    val type = object : TypeToken<List<PricePoint>>() {}.type
                    gson.fromJson<List<PricePoint>>(it, type)
                }
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        tokenData = TokenPerformanceResponse(
                            symbol = symbol,
                            price = cached.price,
                            change24h = cached.change24h,
                            volume24h = cached.volume24h,
                            marketCap = cached.marketCap,
                            history = history
                        )
                    )
                }
                return@launch
            }

            try {
                val data = api.getTokenPerformance(symbol = symbol, days = 7)
                // Persist to cache
                cacheDao.upsert(
                    PriceCacheEntity(
                        symbol = symbol,
                        price = data.price,
                        change24h = data.change24h,
                        volume24h = data.volume24h,
                        marketCap = data.marketCap,
                        historyJson = data.history?.let { gson.toJson(it) }
                    )
                )
                _state.update { it.copy(isLoading = false, tokenData = data) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun loadTopNfts() {
        viewModelScope.launch {
            try {
                val resp = api.getTopNfts()
                _state.update { it.copy(nftCollections = resp.collections) }
            } catch (e: Exception) {
                // NFT load failure is non-fatal
            }
        }
    }

    fun refresh() {
        loadTopNfts()
        val sym = _state.value.tokenData?.symbol ?: "SOL"
        loadToken(sym)
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
}
