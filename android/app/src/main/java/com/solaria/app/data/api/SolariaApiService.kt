package com.solaria.app.data.api

import com.solaria.app.BuildConfig
import com.solaria.app.data.models.*
import retrofit2.http.*

interface SolariaApiService {

    // ── Chat ──────────────────────────────────────────────────
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse

    // ── Balance ───────────────────────────────────────────────
    @GET("api/balance")
    suspend fun getBalance(@Query("wallet") walletAddress: String): BalanceResponse

    // ── Token performance (optional days param for 7d history) ─
    @GET("api/token/performance")
    suspend fun getTokenPerformance(
        @Query("symbol") symbol: String? = null,
        @Query("mint") mint: String? = null,
        @Query("days") days: Int? = null
    ): TokenPerformanceResponse

    // ── Top NFTs ──────────────────────────────────────────────
    @GET("api/nfts/top")
    suspend fun getTopNfts(): NftCollectionsResponse

    // ── Transfer ─────────────────────────────────────────────
    @POST("api/prepare-transfer")
    suspend fun prepareTransfer(@Body request: PrepareTransferRequest): PrepareTransferResponse

    @POST("api/confirm-transfer")
    suspend fun confirmTransfer(@Body request: ConfirmTransferRequest): ConfirmTransferResponse

    // ── LI.FI cross-chain ────────────────────────────────────
    @POST("api/lifi/quote")
    suspend fun getLifiQuote(@Body request: LifiQuoteRequest): LifiQuoteResponse

    @GET("api/lifi/status")
    suspend fun getLifiStatus(@Query("txHash") txHash: String): LifiStatusResponse

    @POST("api/lifi/settle")
    suspend fun settleLifi(@Body body: Map<String, String>): Map<String, String>
}
