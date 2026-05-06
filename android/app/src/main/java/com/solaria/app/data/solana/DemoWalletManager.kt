package com.solaria.app.data.solana

import com.solaria.app.data.db.WalletDao
import com.solaria.app.data.db.WalletEntity
import com.solaria.app.data.firestore.UserDataManager
import com.solaria.app.data.repository.SolariaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoWalletManager @Inject constructor(
    private val keypairManager: SolanaKeypairManager,
    private val rpcClient: SolanaRpcClient,
    private val userDataManager: UserDataManager,
    private val walletDao: WalletDao
) {

    suspend fun generateAndStoreWallet(userId: String): String = withContext(Dispatchers.IO) {
        // 1. Generate keypair
        val publicKey = keypairManager.generateKeypair()
        val privateKey = keypairManager.exportPrivateKey(publicKey) ?: ""

        // 2. Fund with 2 SOL
        try {
            val sig = rpcClient.requestAirdrop(publicKey, 2_000_000_000L)
            rpcClient.confirmTransaction(sig)
        } catch (e: Exception) {
            // Airdrop failed but we still create the wallet as per prompt
        }

        val now = System.currentTimeMillis()

        // 3. Store in Firestore
        val walletData = mapOf(
            "publicKey" to publicKey,
            "privateKey" to privateKey,
            "createdAt" to now,
            "lastAirdropAt" to now
        )
        userDataManager.saveWallet(userId, walletData)

        // 4. Store in Room
        walletDao.disconnectAll()
        walletDao.upsert(
            WalletEntity(
                address = publicKey,
                label = "Main Wallet",
                isConnected = true,
                lastSyncedAt = now
            )
        )

        return@withContext publicKey
    }

    suspend fun loadWalletFromFirestore(userId: String) {
        val data = userDataManager.getWallet(userId) ?: return
        val publicKey = data["publicKey"] as? String ?: return
        val privateKey = data["privateKey"] as? String ?: return

        // Import private key into local storage
        keypairManager.importFromPrivateKey(privateKey)

        // Update Room
        walletDao.disconnectAll()
        walletDao.upsert(
            WalletEntity(
                address = publicKey,
                label = "Main Wallet",
                isConnected = true,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }
}
