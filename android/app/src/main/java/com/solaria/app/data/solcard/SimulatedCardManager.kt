package com.solaria.app.data.solcard

import com.solaria.app.data.firestore.UserDataManager
import com.solaria.app.data.repository.SolariaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimulatedCardManager @Inject constructor(
    private val userDataManager: UserDataManager,
    private val repository: SolariaRepository
) {

    suspend fun getCardDetails(userId: String): Map<String, Any>? {
        return userDataManager.getWallet(userId)?.let { wallet ->
            // In a real app, we'd fetch specific card info
            // For demo, we simulate a card tied to the wallet balance
            val balance = repository.computePortfolioValue()
            mapOf(
                "cardNumber" to "4532 7812 9012 4456",
                "expiry" to "12/28",
                "cvv" to "771",
                "cardholder" to "Solaria User",
                "balance" to balance,
                "status" to "ACTIVE"
            )
        }
    }

    suspend fun authorizeTransaction(userId: String, amountUsd: Double): Boolean {
        val currentBalance = repository.computePortfolioValue()
        if (currentBalance >= amountUsd) {
            // In a real app, we'd deduct or lock funds
            return true
        }
        return false
    }
}
