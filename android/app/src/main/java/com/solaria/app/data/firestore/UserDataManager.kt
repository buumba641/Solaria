package com.solaria.app.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataManager @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    suspend fun saveProfile(userId: String, profile: Map<String, Any>) {
        db.collection("users").document(userId).collection("profile")
            .document("info").set(profile).await()
    }

    suspend fun saveWallet(userId: String, wallet: Map<String, Any>) {
        db.collection("users").document(userId).collection("wallet")
            .document("data").set(wallet).await()
    }

    suspend fun getWallet(userId: String): Map<String, Any>? {
        val doc = db.collection("users").document(userId).collection("wallet")
            .document("data").get().await()
        return doc.data
    }

    suspend fun saveSolCard(userId: String, card: Map<String, Any>) {
        db.collection("users").document(userId).collection("solcard")
            .document("info").set(card).await()
    }

    suspend fun addBitrefillOrder(userId: String, order: Map<String, Any>) {
        db.collection("users").document(userId).collection("bitrefill")
            .document("orders").update("list", com.google.firebase.firestore.FieldValue.arrayUnion(order))
            .await()
    }
}
