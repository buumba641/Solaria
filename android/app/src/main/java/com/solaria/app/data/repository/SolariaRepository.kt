package com.solaria.app.data.repository

import com.solaria.app.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository — single source of truth for all app data.
 * Reads/writes to local Room DB. Future backends write to the same tables.
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
    private val chatDao: ChatDao
) {
    // ── Wallet ─────────────────────────────────────────
    fun observeConnectedWallet(): Flow<WalletEntity?> = walletDao.observeConnected()
    suspend fun connectWallet(address: String) {
        walletDao.disconnectAll()
        val existing = walletDao.getByAddress(address)
        if (existing != null) walletDao.connect(address)
        else walletDao.upsert(WalletEntity(address = address, isConnected = true))
        updateSyncTimestamp("wallets")
    }
    suspend fun disconnectWallet() { walletDao.disconnectAll() }
    suspend fun getWalletBalance(address: String): WalletEntity? = walletDao.getByAddress(address)

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
    fun observeP2PTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeP2P()

    suspend fun createTransaction(
        fromAddress: String, toAddress: String, amountSol: Double,
        type: String, paymentMethod: String, description: String?,
        isOffline: Boolean = false
    ): TransactionEntity {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(), fromAddress = fromAddress,
            toAddress = toAddress, amountSol = amountSol,
            amountLamports = (amountSol * 1_000_000_000).toLong(),
            type = type, status = if (isOffline) "PENDING_SYNC" else "PENDING",
            paymentMethod = paymentMethod, description = description,
            createdAt = System.currentTimeMillis(), isOffline = isOffline
        )
        transactionDao.upsert(tx)
        updateSyncTimestamp("transactions")
        return tx
    }

    suspend fun confirmTransaction(id: String, txHash: String?) {
        transactionDao.updateStatus(id, "CONFIRMED", txHash, System.currentTimeMillis())
    }

    suspend fun getPendingSyncTransactions(): List<TransactionEntity> =
        transactionDao.getPendingSync()

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

    // ── Computed ────────────────────────────────────────
    suspend fun computePortfolioValue(): Double {
        val wallet = walletDao.observeConnected().first() ?: return 0.0
        val solPrice = priceCacheDao.getBySymbol("SOL")?.price ?: 0.0
        val solValue = wallet.balanceSol * solPrice
        val tokens = tokenBalanceDao.observeForWallet(wallet.address).first()
        val tokenValue = tokens.sumOf { it.usdValue ?: 0.0 }
        return solValue + tokenValue
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
