package com.solaria.app.data.solana

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct JSON-RPC client for Solana DevNet.
 * Implements the core RPC methods needed for wallet operations:
 * getBalance, requestAirdrop, getLatestBlockhash, sendTransaction.
 *
 * Equivalent to running solana config set --url devnet + CLI commands.
 */
@Singleton
class SolanaRpcClient @Inject constructor() {

    companion object {
        const val DEVNET_URL = "https://api.devnet.solana.com"
        const val LAMPORTS_PER_SOL = 1_000_000_000L
        private val JSON_MEDIA = "application/json".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val requestId = AtomicInteger(1)

    // ── Core RPC Methods ──────────────────────────────────────

    /**
     * Get wallet balance in lamports.
     * Equivalent to solana balance
     */
    suspend fun getBalance(address: String): Long = withContext(Dispatchers.IO) {
        val response = rpcCall("getBalance", listOf(address))
        val result = response.getAsJsonObject("result")
        result.get("value").asLong
    }

    /**
     * Get balance in SOL (convenience method).
     */
    suspend fun getBalanceSol(address: String): Double {
        return getBalance(address).toDouble() / LAMPORTS_PER_SOL
    }

    /**
     * Request an airdrop of SOL on DevNet.
     * Equivalent to solana airdrop
     */
    suspend fun requestAirdrop(address: String, lamports: Long): String = withContext(Dispatchers.IO) {
        val response = rpcCall("requestAirdrop", listOf(address, lamports))
        response.get("result").asString
    }

    /**
     * Get the latest blockhash for transaction signing.
     */
    suspend fun getLatestBlockhash(): Pair<String, Long> = withContext(Dispatchers.IO) {
        val params = listOf(mapOf("commitment" to "finalized"))
        val response = rpcCall("getLatestBlockhash", params)
        val result = response.getAsJsonObject("result")
        val value = result.getAsJsonObject("value")
        val blockhash = value.get("blockhash").asString
        val lastValidHeight = value.get("lastValidBlockHeight").asLong
        Pair(blockhash, lastValidHeight)
    }

    /**
     * Send a signed transaction to the network.
     */
    suspend fun sendTransaction(signedTxBase64: String): String = withContext(Dispatchers.IO) {
        val params = listOf(
            signedTxBase64,
            mapOf(
                "encoding" to "base64",
                "preflightCommitment" to "confirmed"
            )
        )
        val response = rpcCall("sendTransaction", params)
        val result = response.get("result")
        if (result == null || result.isJsonNull) {
            val error = response.getAsJsonObject("error")
            throw SolanaRpcException(
                error?.get("message")?.asString ?: "Unknown RPC error",
                error?.get("code")?.asInt ?: -1
            )
        }
        result.asString
    }

    /**
     * Confirm a transaction by polling its status.
     */
    suspend fun confirmTransaction(
        signature: String,
        maxRetries: Int = 30,
        delayMs: Long = 1000
    ): Boolean = withContext(Dispatchers.IO) {
        repeat(maxRetries) {
            try {
                val response = rpcCall("getSignatureStatuses", listOf(listOf(signature)))
                val result = response.getAsJsonObject("result")
                val valueArray = result.getAsJsonArray("value")
                val value = if (valueArray != null && valueArray.size() > 0) valueArray.get(0) else null

                if (value != null && !value.isJsonNull) {
                    val status = value.asJsonObject
                    val err = status.get("err")
                    if (err != null && !err.isJsonNull) {
                        throw SolanaRpcException("Transaction failed: $err", -1)
                    }
                    val confirmationStatus = status.get("confirmationStatus")?.asString
                    if (confirmationStatus == "confirmed" || confirmationStatus == "finalized") {
                        return@withContext true
                    }
                }
            } catch (e: SolanaRpcException) {
                throw e
            } catch (_: Exception) {
                // Retry on network errors
            }
            kotlinx.coroutines.delay(delayMs)
        }
        throw SolanaRpcException("Transaction confirmation timeout after ${maxRetries}s", -1)
    }

    /**
     * Get the SOL price from CoinGecko (free, no API key).
     */
    suspend fun getSolPrice(): SolPriceData = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.coingecko.com/api/v3/simple/price?ids=solana&vs_currencies=usd&include_24hr_change=true&include_24hr_vol=true&include_market_cap=true")
            .get()
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from CoinGecko")
        val root = JsonParser.parseString(body).asJsonObject
        val json = root.getAsJsonObject("solana")
        SolPriceData(
            price = json.get("usd").asDouble,
            change24h = json.get("usd_24h_change")?.asDouble ?: 0.0,
            volume24h = json.get("usd_24h_vol")?.asDouble,
            marketCap = json.get("usd_market_cap")?.asDouble
        )
    }

    // ── Internal ──────────────────────────────────────────────

    private fun rpcCall(method: String, params: List<Any>): JsonObject {
        val requestBody = mapOf(
            "jsonrpc" to "2.0",
            "id" to requestId.getAndIncrement(),
            "method" to method,
            "params" to params
        )
        val json = gson.toJson(requestBody)
        val request = Request.Builder()
            .url(DEVNET_URL)
            .post(json.toRequestBody(JSON_MEDIA))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw SolanaRpcException("Empty RPC response", -1)

        val parsed = JsonParser.parseString(responseBody).asJsonObject
        // Check for top-level RPC error (but not in sendTransaction which checks itself)
        val error = parsed.getAsJsonObject("error")
        if (error != null) {
            throw SolanaRpcException(
                error.get("message")?.asString ?: "RPC error",
                error.get("code")?.asInt ?: -1
            )
        }
        return parsed
    }
}

data class SolPriceData(
    val price: Double,
    val change24h: Double,
    val volume24h: Double?,
    val marketCap: Double?
)

class SolanaRpcException(message: String, val code: Int) : Exception(message)
