package com.solaria.app.data.models

import com.google.gson.annotations.SerializedName

// ──────────────────────────────────────────────────────────────
// /api/chat
// ──────────────────────────────────────────────────────────────

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("walletAddress") val walletAddress: String,
    @SerializedName("conversationId") val conversationId: String? = null
)

data class ChatResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("action") val action: BotAction? = null,
    @SerializedName("conversationId") val conversationId: String? = null
)

data class BotAction(
    @SerializedName("type") val type: String,          // TRANSFER | CROSS_CHAIN | NONE
    @SerializedName("payload") val payload: TransactionPayload? = null,
    @SerializedName("lifiRequest") val lifiRequest: LifiRequest? = null
)

data class TransactionPayload(
    @SerializedName("unsignedTxBase64") val unsignedTxBase64: String,
    @SerializedName("to") val to: String,
    @SerializedName("amountSol") val amountSol: Double,
    @SerializedName("feeSol") val feeSol: Double? = null
)

data class LifiRequest(
    @SerializedName("requestId") val requestId: String,
    @SerializedName("walletConnectUri") val walletConnectUri: String,
    @SerializedName("fromChain") val fromChain: String,
    @SerializedName("fromToken") val fromToken: String,
    @SerializedName("amount") val amount: String
)

// ──────────────────────────────────────────────────────────────
// /api/balance
// ──────────────────────────────────────────────────────────────

data class BalanceResponse(
    @SerializedName("sol") val sol: Double,
    @SerializedName("tokens") val tokens: List<SplToken>? = null
)

data class SplToken(
    @SerializedName("mint") val mint: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("usdValue") val usdValue: Double? = null
)

// ──────────────────────────────────────────────────────────────
// /api/token/performance
// ──────────────────────────────────────────────────────────────

data class TokenPerformanceResponse(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("mint") val mint: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("change24h") val change24h: Double,
    @SerializedName("volume24h") val volume24h: Double? = null,
    @SerializedName("marketCap") val marketCap: Double? = null,
    @SerializedName("history") val history: List<PricePoint>? = null
)

data class PricePoint(
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("price") val price: Double
)

// ──────────────────────────────────────────────────────────────
// /api/nfts/top
// ──────────────────────────────────────────────────────────────

data class NftCollectionsResponse(
    @SerializedName("collections") val collections: List<NftCollection>
)

data class NftCollection(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String? = null,
    @SerializedName("floorPrice") val floorPrice: Double,
    @SerializedName("volume24h") val volume24h: Double? = null,
    @SerializedName("change24h") val change24h: Double? = null
)

// ──────────────────────────────────────────────────────────────
// /api/prepare-transfer
// ──────────────────────────────────────────────────────────────

data class PrepareTransferRequest(
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("amountSol") val amountSol: Double
)

data class PrepareTransferResponse(
    @SerializedName("unsignedTxBase64") val unsignedTxBase64: String,
    @SerializedName("feeSol") val feeSol: Double? = null
)

// ──────────────────────────────────────────────────────────────
// /api/confirm-transfer
// ──────────────────────────────────────────────────────────────

data class ConfirmTransferRequest(
    @SerializedName("signedTxBase64") val signedTxBase64: String
)

data class ConfirmTransferResponse(
    @SerializedName("txHash") val txHash: String,
    @SerializedName("status") val status: String
)

// ──────────────────────────────────────────────────────────────
// /api/lifi/quote
// ──────────────────────────────────────────────────────────────

data class LifiQuoteRequest(
    @SerializedName("fromChain") val fromChain: String,
    @SerializedName("toChain") val toChain: String,
    @SerializedName("fromToken") val fromToken: String,
    @SerializedName("toToken") val toToken: String,
    @SerializedName("amount") val amount: String,
    @SerializedName("fromAddress") val fromAddress: String
)

data class LifiQuoteResponse(
    @SerializedName("requestId") val requestId: String,
    @SerializedName("walletConnectUri") val walletConnectUri: String,
    @SerializedName("estimate") val estimate: LifiEstimate? = null
)

data class LifiEstimate(
    @SerializedName("toAmount") val toAmount: String,
    @SerializedName("executionDuration") val executionDuration: Int
)

// ──────────────────────────────────────────────────────────────
// /api/lifi/status
// ──────────────────────────────────────────────────────────────

data class LifiStatusResponse(
    @SerializedName("status") val status: String,     // PENDING | DONE | FAILED
    @SerializedName("txHash") val txHash: String? = null,
    @SerializedName("substatus") val substatus: String? = null
)
