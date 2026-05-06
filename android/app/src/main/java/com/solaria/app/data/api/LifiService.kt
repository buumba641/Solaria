package com.solaria.app.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifiService @Inject constructor() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://li.quest/v1"

    suspend fun getQuote(
        fromChain: String, // "SOL"
        toChain: String,   // "SOL"
        fromToken: String, // Mint address
        toToken: String,   // USDC Mint address
        fromAmount: String,
        fromAddress: String
    ): JsonObject? {
        val url = "$baseUrl/quote?fromChain=$fromChain&toChain=$toChain&fromToken=$fromToken&toToken=$toToken&fromAmount=$fromAmount&fromAddress=$fromAddress"
        
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
            
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            JsonParser.parseString(body).asJsonObject
        } catch (e: Exception) {
            null
        }
    }
}
