package com.solaria.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthManager @Inject constructor() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun signUp(email: String, password: String): String? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    suspend fun signIn(email: String, password: String): String? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.uid
        } catch (e: Exception) {
            throw mapFirebaseError(e)
        }
    }

    fun getCurrentUser(): String? {
        return auth.currentUser?.uid
    }

    fun signOut() {
        auth.signOut()
    }

    private fun mapFirebaseError(e: Exception): Exception {
        if (e is FirebaseAuthException) {
            val message = when (e.errorCode) {
                "ERROR_WEAK_PASSWORD" -> "Password is too weak"
                "ERROR_INVALID_EMAIL" -> "Invalid email address"
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email is already in use"
                "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                "ERROR_USER_NOT_FOUND" -> "User not found"
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error"
                else -> e.message ?: "Authentication failed"
            }
            return Exception(message)
        }
        return e
    }
}
