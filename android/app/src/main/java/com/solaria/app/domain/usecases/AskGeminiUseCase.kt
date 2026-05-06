package com.solaria.app.domain.usecases

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.solaria.app.BuildConfig
import com.solaria.app.data.repository.SolariaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AskGeminiUseCase @Inject constructor(
    private val repository: SolariaRepository
) {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun ask(userMessage: String): String {
        val summary = repository.getPortfolioSummary()
        
        val systemPrompt = """
            You are Solaria AI, a financial advisor for a Solana-based crypto app.
            Your goal is to provide privacy-safe, insightful financial advice based on the user's local data.
            
            Current User Data:
            - Total Portfolio Value: ${summary["totalValue"]} USD
            - SOL Balance: ${summary["solAmount"]} SOL
            - Tokens: ${summary["tokens"]}
            - Recent Activity: ${summary["recentTransactions"]}
            
            Guidelines:
            1. Never mention specific wallet addresses or private keys.
            2. Be helpful but cautious with financial advice.
            3. If the user asks for a transaction, remind them they can say "Send [amount] SOL to [name]".
            4. Keep responses concise and formatted for a mobile chat screen.
            
            User Question: $userMessage
        """.trimIndent()

        return try {
            val response = model.generateContent(systemPrompt)
            response.text ?: "I'm sorry, I couldn't generate a response."
        } catch (e: Exception) {
            "Error connecting to Gemini: ${e.message}"
        }
    }
}
