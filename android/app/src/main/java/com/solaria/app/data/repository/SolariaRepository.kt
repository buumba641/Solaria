package com.solaria.app.data.repository

import com.solaria.app.data.db.*
import com.solaria.app.data.solana.SolanaKeypairManager
import com.solaria.app.data.solana.SolanaRpcClient
import com.solaria.app.data.solana.SolanaTransactionBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository — single source of truth for all app data.
 * Now integrated with real Solana DevNet operations:
 * - Keypair generation (like `solana-keygen new`)
 * - Real balance from DevNet RPC
 * - Real airdrop from DevNet faucet
 * - Real SOL transfers signed on-device
 */
@Singleton
class SolariaRepository @Inject constructor(
    private val walletDao: WalletDao,
    private val tokenBalanceDao: TokenBalanceDao,
    private val transactionDao: TransactionDao,
    private val nftCollectionDao: NftCollectionDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val priceCacheDao: PriceCacheDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val chatDao: ChatDao,
    private val userDataManager: com.solaria.app.data.firestore.UserDataManager,
    val keypairManager: SolanaKeypairManager,
    val rpcClient: SolanaRpcClient,
    val txBuilder: SolanaTransactionBuilder
) {
    companion object {
        const val NATIVE_SOL_MINT = "11111111111111111111111111111111" // Native SOL representation
    }

    /**
     * Request a real DevNet airdrop and sync the updated balance to Firestore.
     * This is the AI-friendly wrapper around requestAirdrop().
     */
    suspend fun airdropAndSync(userId: String, amountSol: Double): String {
        val wallet = walletDao.observeConnected().first()
            ?: throw IllegalStateException("No wallet connected")
        val signature = requestAirdrop(wallet.address, amountSol)
        // Sync updated balance to Firestore
        val updatedWallet = walletDao.getByAddress(wallet.address)
        if (updatedWallet != null && userId.isNotBlank()) {
            userDataManager.saveWallet(userId, mapOf("balanceSol" to updatedWallet.balanceSol))
        }
        return signature
    }

    // ── Wallet ─────────────────────────────────────────
    fun observeConnectedWallet(): Flow<WalletEntity?> = walletDao.observeConnected()

    /**
     * Generate a new Solana wallet keypair on-device.
     * Equivalent to `solana-keygen new`.
     * Private key is stored in EncryptedSharedPreferences.
     */
    suspend fun generateWallet(): String {
        val address = keypairManager.generateKeypair()
        walletDao.disconnectAll()
        walletDao.upsert(WalletEntity(
            address = address,
            label = "Main Wallet",
            isConnected = true,
            lastSyncedAt = System.currentTimeMillis()
        ))
        updateSyncTimestamp("wallets")
        return address
    }

    suspend fun connectWallet(address: String) {
        walletDao.disconnectAll()
        val existing = walletDao.getByAddress(address)
        if (existing != null) walletDao.connect(address)
        else walletDao.upsert(WalletEntity(address = address, isConnected = true))
        updateSyncTimestamp("wallets")
    }

    suspend fun disconnectWallet() { walletDao.disconnectAll() }
    suspend fun getWalletBalance(address: String): WalletEntity? = walletDao.getByAddress(address)

    /**
     * Fetch real balance from Solana DevNet RPC and update the local DB.
     * Equivalent to `solana balance`.
     */
    suspend fun refreshBalance(address: String): Double {
        val lamports = rpcClient.getBalance(address)
        val sol = lamports.toDouble() / SolanaRpcClient.LAMPORTS_PER_SOL
        walletDao.updateBalance(address, sol, lamports, System.currentTimeMillis())
        updateSyncTimestamp("wallets")
        return sol
    }

    /**
     * Request an airdrop of SOL from DevNet faucet.
     * Equivalent to `solana airdrop <amount>`.
     *
     * @param address Wallet address to fund
     * @param amountSol Amount in SOL (max ~2 per request on DevNet)
     * @return Transaction signature
     */
    suspend fun requestAirdrop(address: String, amountSol: Double): String {
        val lamports = (amountSol * SolanaRpcClient.LAMPORTS_PER_SOL).toLong()
        val signature = rpcClient.requestAirdrop(address, lamports)
        // Wait for confirmation then refresh balance
        rpcClient.confirmTransaction(signature, maxRetries = 30, delayMs = 1500)
        refreshBalance(address)
        return signature
    }

    /**
     * Send SOL to another address on DevNet.
     * Builds the transaction locally, signs with the on-device keypair,
     * and broadcasts to the network.
     *
     * @return Transaction signature
     */
    suspend fun sendSol(
        fromAddress: String,
        toAddress: String,
        amountSol: Double
    ): String {
        val lamports = (amountSol * SolanaRpcClient.LAMPORTS_PER_SOL).toLong()

        // Get recent blockhash
        val (blockhash, _) = rpcClient.getLatestBlockhash()

        // Build and sign the transaction
        val signedTxBase64 = txBuilder.buildTransferTransaction(
            fromAddress = fromAddress,
            toAddress = toAddress,
            lamports = lamports,
            recentBlockhash = blockhash
        )

        // Send to network
        val txSignature = rpcClient.sendTransaction(signedTxBase64)

        // Record in local DB
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            fromAddress = fromAddress,
            toAddress = toAddress,
            amountSol = amountSol,
            amountLamports = lamports,
            feeSol = 0.000005,
            type = "SEND",
            status = "PENDING",
            paymentMethod = "DEVNET",
            txHash = txSignature,
            description = "Send ${"%.4f".format(amountSol)} SOL",
            createdAt = System.currentTimeMillis()
        )
        transactionDao.upsert(tx)

        // Confirm and update status
        try {
            rpcClient.confirmTransaction(txSignature)
            transactionDao.updateStatus(tx.id, "CONFIRMED", txSignature, System.currentTimeMillis())
            refreshBalance(fromAddress)
        } catch (e: Exception) {
            transactionDao.updateStatus(tx.id, "FAILED", txSignature, System.currentTimeMillis())
            throw e
        }

        updateSyncTimestamp("transactions")
        return txSignature
    }

    /**
     * Fetch real SOL price from CoinGecko and update local cache.
     */
    suspend fun refreshSolPrice() {
        try {
            val priceData = rpcClient.getSolPrice()
            priceCacheDao.upsert(PriceCacheEntity(
                symbol = "SOL",
                price = priceData.price,
                change24h = priceData.change24h,
                volume24h = priceData.volume24h,
                marketCap = priceData.marketCap,
                historyJson = null,
                updatedAtMs = System.currentTimeMillis()
            ))
            updateSyncTimestamp("prices")
        } catch (_: Exception) {
            // Price fetch failed — keep cached data
        }
    }

    // ── Token Balances ─────────────────────────────────
    fun observeTokenBalances(wallet: String): Flow<List<TokenBalanceEntity>> =
        tokenBalanceDao.observeForWallet(wallet)

    // ── Price Cache ────────────────────────────────────
    fun observeAllPrices(): Flow<List<PriceCacheEntity>> = priceCacheDao.observeAll()
    suspend fun getPrice(symbol: String): PriceCacheEntity? = priceCacheDao.getBySymbol(symbol)

    // ── Transactions ───────────────────────────────────
    fun observeRecentTransactions(limit: Int = 20): Flow<List<TransactionEntity>> =
        transactionDao.observeRecent(limit)
    fun observeAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeAll()


    suspend fun createTransaction(
        fromAddress: String, toAddress: String, amountSol: Double,
        type: String, paymentMethod: String, description: String?
    ): TransactionEntity {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(), fromAddress = fromAddress,
            toAddress = toAddress, amountSol = amountSol,
            amountLamports = (amountSol * 1_000_000_000).toLong(),
            type = type, status = "PENDING",
            paymentMethod = paymentMethod, description = description,
            createdAt = System.currentTimeMillis()
        )
        transactionDao.upsert(tx)
        updateSyncTimestamp("transactions")
        return tx
    }

    suspend fun confirmTransaction(id: String, txHash: String?) {
        transactionDao.updateStatus(id, "CONFIRMED", txHash, System.currentTimeMillis())
    }



    // ── NFT Collections ────────────────────────────────
    fun observeNftCollections(): Flow<List<NftCollectionEntity>> = nftCollectionDao.observeAll()

    // ── Payment Methods ────────────────────────────────
    fun observePaymentMethods(): Flow<List<PaymentMethodEntity>> = paymentMethodDao.observeAll()
    fun observeActivePaymentMethods(): Flow<List<PaymentMethodEntity>> = paymentMethodDao.observeActive()

    // ── Sync Metadata ──────────────────────────────────
    fun observeAllSyncMetadata(): Flow<List<SyncMetadataEntity>> = syncMetadataDao.observeAll()
    fun observeSyncForType(type: String): Flow<SyncMetadataEntity?> = syncMetadataDao.observeByType(type)

    suspend fun updateSyncTimestamp(dataType: String) {
        val existing = syncMetadataDao.getByType(dataType)
        if (existing != null) {
            syncMetadataDao.updateSync(dataType, System.currentTimeMillis(), "synced", existing.recordCount)
        } else {
            syncMetadataDao.upsert(SyncMetadataEntity(dataType, System.currentTimeMillis(), "synced", 0))
        }
    }

    // ── Chat ───────────────────────────────────────────
    fun observeChatMessages(conversationId: String): Flow<List<ChatMessageEntity>> =
        chatDao.observeMessages(conversationId)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long = chatDao.insert(message)

    // ── Price History ─────────────────────────────────
    fun observePriceHistory(coinId: String, tier: String): Flow<List<*>> {
        return when (tier) {
            "hourly" -> priceHistoryDao.observeHourly(coinId)
            "daily" -> priceHistoryDao.observeDaily(coinId)
            "weekly" -> priceHistoryDao.observeWeekly(coinId)
            else -> priceHistoryDao.observeDaily(coinId)
        }
    }

    // ── Portfolio Metrics ──────────────────────────────
    suspend fun getPortfolioSummary(): Map<String, Any> {
        val wallet = walletDao.observeConnected().first() ?: return emptyMap()
        val prices = priceCacheDao.observeAll().first()
        val tokens = tokenBalanceDao.observeForWallet(wallet.address).first()
        
        val solPrice = prices.find { it.symbol == "SOL" }?.price ?: 0.0
        val solValue = wallet.balanceSol * solPrice
        val tokenValue = tokens.sumOf { it.usdValue ?: 0.0 }
        val totalValue = solValue + tokenValue
        
        return mapOf(
            "totalValue" to totalValue,
            "solAmount" to wallet.balanceSol,
            "tokens" to tokens.map { mapOf("symbol" to it.symbol, "amount" to it.amount) },
            "recentTransactions" to transactionDao.observeRecent(10).first().map {
                mapOf("type" to it.type, "amount" to it.amountSol, "timestamp" to it.createdAt)
            }
        )
    }

    suspend fun computePortfolioValue(): Double {
        val summary = getPortfolioSummary()
        return summary["totalValue"] as? Double ?: 0.0
    }

    fun formatSyncAge(timestampMs: Long): String {
        val diff = System.currentTimeMillis() - timestampMs
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hr ago"
            else -> "${diff / 86_400_000} days ago"
        }
    }
}
