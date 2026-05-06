package com.solaria.app.data.api

import com.solaria.app.data.models.*
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSolariaApiService @Inject constructor() : SolariaApiService {

    override suspend fun chat(request: ChatRequest): ChatResponse {
        delay(1000)
        return when {
            request.message.contains("send", ignoreCase = true) || request.message.contains("transfer", ignoreCase = true) -> {
                ChatResponse(
                    reply = "Sure, I can help you with that. I've prepared a transfer of 0.1 SOL to a sample address. Please review and approve.",
                    action = BotAction(
                        type = "TRANSFER",
                        payload = TransactionPayload(
                            unsignedTxBase64 = "AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAgAIAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAJIdS7Y9yv9z8Y1v7Kz9y9v9y9v9y9v9y9v9y9v9y9v9y8=",
                            to = "9WzDXwByocGv8ZH6Q1nF3h7iV4d6m9uX6k6v6x6y6z6A",
                            amountSol = 0.1,
                            feeSol = 0.000005
                        )
                    )
                )
            }
            request.message.contains("balance", ignoreCase = true) -> {
                ChatResponse(reply = "Your current balance is 1.5 SOL and 100 USDC.")
            }
            else -> {
                ChatResponse(reply = "Hello! I am Solaria, your voice-first Solana assistant. You can ask me to check balances, market performance, or send tokens.")
            }
        }
    }

    override suspend fun getBalance(walletAddress: String): BalanceResponse {
        delay(500)
        return BalanceResponse(
            sol = 1.5,
            tokens = listOf(
                SplToken(mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v", symbol = "USDC", amount = 100.0, usdValue = 100.0),
                SplToken(mint = "Es9vMFrzaDCSTv4asS98TM2dkMvBDSECPpOGv1ta169", symbol = "USDT", amount = 50.0, usdValue = 50.0)
            )
        )
    }

    override suspend fun getTokenPerformance(symbol: String?, mint: String?, days: Int?): TokenPerformanceResponse {
        delay(500)
        return TokenPerformanceResponse(
            symbol = symbol ?: "SOL",
            price = 145.20,
            change24h = 5.4,
            history = List(10) { i ->
                PricePoint(timestamp = System.currentTimeMillis() - (10 - i) * 3600000, price = 140.0 + i * 0.5)
            }
        )
    }

    override suspend fun getTopNfts(): NftCollectionsResponse {
        delay(500)
        return NftCollectionsResponse(
            collections = listOf(
                NftCollection(symbol = "MAD", name = "Mad Lads", floorPrice = 80.5, change24h = 2.1),
                NftCollection(symbol = "SMB", name = "Solana Monkey Business", floorPrice = 45.0, change24h = -1.2)
            )
        )
    }

    override suspend fun prepareTransfer(request: PrepareTransferRequest): PrepareTransferResponse {
        delay(500)
        return PrepareTransferResponse(
            unsignedTxBase64 = "AgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAgAIAAoAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAJIdS7Y9yv9z8Y1v7Kz9y9v9y9v9y9v9y9v9y9v9y9v9y8=",
            feeSol = 0.000005
        )
    }

    override suspend fun confirmTransfer(request: ConfirmTransferRequest): ConfirmTransferResponse {
        delay(1000)
        return ConfirmTransferResponse(
            txHash = "5GzDXwByocGv8ZH6Q1nF3h7iV4d6m9uX6k6v6x6y6z6A7B8C9D0E1F2G3H4I5J6K",
            status = "CONFIRMED"
        )
    }

    override suspend fun getLifiQuote(request: LifiQuoteRequest): LifiQuoteResponse {
        delay(1000)
        return LifiQuoteResponse(
            requestId = "mock-lifi-req",
            walletConnectUri = "wc:mock-uri",
            estimate = LifiEstimate(toAmount = "0.09", executionDuration = 120)
        )
    }

    override suspend fun getLifiStatus(txHash: String): LifiStatusResponse {
        delay(500)
        return LifiStatusResponse(status = "DONE", txHash = txHash)
    }

    override suspend fun settleLifi(body: Map<String, String>): Map<String, String> {
        delay(500)
        return mapOf("status" to "success")
    }
}
