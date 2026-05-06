package com.solaria.app.data.market

/**
 * Curated fixed list of 22 coins tracked by Solaria.
 * All CoinGecko API calls use this list exclusively.
 * Never fetch or display any coin outside this list.
 */
object CuratedCoinList {

    data class CoinInfo(
        val id: String,           // CoinGecko API ID
        val symbol: String,       // Ticker symbol
        val name: String,         // Display name
        val isSolanaEcosystem: Boolean
    )

    // ── Solana Ecosystem (12 coins) ──────────────────────────
    private val solanaCoins = listOf(
        CoinInfo("solana", "SOL", "Solana", true),
        CoinInfo("bonk", "BONK", "Bonk", true),
        CoinInfo("jupiter-exchange-solana", "JUP", "Jupiter", true),
        CoinInfo("jito-governance-token", "JTO", "Jito", true),
        CoinInfo("raydium", "RAY", "Raydium", true),
        CoinInfo("orca", "ORCA", "Orca", true),
        CoinInfo("pyth-network", "PYTH", "Pyth Network", true),
        CoinInfo("marinade", "MNDE", "Marinade", true),
        CoinInfo("render-token", "RENDER", "Render", true),
        CoinInfo("helium", "HNT", "Helium", true),
        CoinInfo("helium-mobile", "MOBILE", "Helium Mobile", true),
        CoinInfo("dogwifcoin", "WIF", "dogwifhat", true),
    )

    // ── Blue Chips (10 coins) ────────────────────────────────
    private val blueChipCoins = listOf(
        CoinInfo("bitcoin", "BTC", "Bitcoin", false),
        CoinInfo("ethereum", "ETH", "Ethereum", false),
        CoinInfo("litecoin", "LTC", "Litecoin", false),
        CoinInfo("ripple", "XRP", "XRP", false),
        CoinInfo("binancecoin", "BNB", "BNB", false),
        CoinInfo("cardano", "ADA", "Cardano", false),
        CoinInfo("dogecoin", "DOGE", "Dogecoin", false),
        CoinInfo("avalanche-2", "AVAX", "Avalanche", false),
        CoinInfo("polkadot", "DOT", "Polkadot", false),
        CoinInfo("chainlink", "LINK", "Chainlink", false),
    )

    /** All 22 tracked coins */
    val allCoins: List<CoinInfo> = solanaCoins + blueChipCoins

    /** Solana ecosystem coins only */
    val solanaEcosystem: List<CoinInfo> = solanaCoins

    /** Blue chip coins only */
    val blueChips: List<CoinInfo> = blueChipCoins

    /** Comma-separated CoinGecko IDs for batch API calls */
    val coinGeckoIds: String = allCoins.joinToString(",") { it.id }

    /** Map from CoinGecko ID → CoinInfo for quick lookup */
    val byId: Map<String, CoinInfo> = allCoins.associateBy { it.id }

    /** Map from symbol → CoinInfo for quick lookup */
    val bySymbol: Map<String, CoinInfo> = allCoins.associateBy { it.symbol }

    /** CoinGecko free API price URL for all 22 coins */
    val priceUrl: String =
        "https://api.coingecko.com/api/v3/simple/price" +
        "?ids=$coinGeckoIds" +
        "&vs_currencies=usd" +
        "&include_24hr_change=true"

    /** CoinGecko market chart range URL builder */
    fun marketChartRangeUrl(coinId: String, fromTimestamp: Long, toTimestamp: Long): String =
        "https://api.coingecko.com/api/v3/coins/$coinId/market_chart/range" +
        "?vs_currency=usd&from=$fromTimestamp&to=$toTimestamp"
}
