package com.solaria.app.data.bitrefill

import com.solaria.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BitrefillService @Inject constructor() {

    private val client = OkHttpClient()
    private val baseUrl = "https://api-stage.bitrefill.com/v2" // Using stage for demo

    suspend fun createInvoice(operatorSlug: String, amount: Double): String? {
        val json = JSONObject().apply {
            put("operatorSlug", operatorSlug)
            put("value", amount)
            put("currency", "USD")
        }
        
        val request = Request.Builder()
            .url("$baseUrl/invoices")
            .post(json.toString().toRequestBody())
            .addHeader("Authorization", "Bearer ${BuildConfig.BITREFILL_API_KEY}")
            .build()
            
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            JSONObject(body).getString("id")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getInvoiceStatus(invoiceId: String): String {
        val request = Request.Builder()
            .url("$baseUrl/invoices/$invoiceId")
            .get()
            .addHeader("Authorization", "Bearer ${BuildConfig.BITREFILL_API_KEY}")
            .build()
            
        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            JSONObject(body).getString("status")
        } catch (e: Exception) {
            "ERROR"
        }
    }
}
