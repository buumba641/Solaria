package com.solaria.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────
// Wallet DAO
// ─────────────────────────────────────────────────────────────

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY label ASC")
    fun observeAll(): Flow<List<WalletEntity>>

    @Query("SELECT * FROM wallets WHERE isConnected = 1 LIMIT 1")
    fun observeConnected(): Flow<WalletEntity?>

    @Query("SELECT * FROM wallets WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): WalletEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(wallet: WalletEntity)

    @Query("UPDATE wallets SET isConnected = 0")
    suspend fun disconnectAll()

    @Query("UPDATE wallets SET isConnected = 1 WHERE address = :address")
    suspend fun connect(address: String)

    @Query("UPDATE wallets SET balanceSol = :sol, balanceLamports = :lamports, lastSyncedAt = :syncedAt WHERE address = :address")
    suspend fun updateBalance(address: String, sol: Double, lamports: Long, syncedAt: Long = System.currentTimeMillis())
}

// ─────────────────────────────────────────────────────────────
// Token Balance DAO
// ─────────────────────────────────────────────────────────────

@Dao
interface TokenBalanceDao {
    @Query("SELECT * FROM token_balances WHERE walletAddress = :wallet ORDER BY symbol ASC")
    fun observeForWallet(wallet: String): Flow<List<TokenBalanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tokens: List<TokenBalanceEntity>)

    @Query("DELETE FROM token_balances WHERE walletAddress = :wallet")
    suspend fun clearForWallet(wallet: String)
}

// ─────────────────────────────────────────────────────────────
// Transaction DAO
// ─────────────────────────────────────────────────────────────

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING_SYNC'")
    suspend fun getPendingSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type IN ('P2P_SEND', 'P2P_RECEIVE') ORDER BY createdAt DESC")
    fun observeP2P(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: TransactionEntity)

    @Query("UPDATE transactions SET status = :status, txHash = :txHash, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, txHash: String? = null, syncedAt: Long? = null)
}

// ─────────────────────────────────────────────────────────────
// NFT Collection DAO
// ─────────────────────────────────────────────────────────────

@Dao
interface NftCollectionDao {
    @Query("SELECT * FROM nft_collections ORDER BY floorPrice DESC")
    fun observeAll(): Flow<List<NftCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(collections: List<NftCollectionEntity>)

    @Query("DELETE FROM nft_collections")
    suspend fun clearAll()
}

// ─────────────────────────────────────────────────────────────
// Sync Metadata DAO
// ─────────────────────────────────────────────────────────────

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata ORDER BY dataType ASC")
    fun observeAll(): Flow<List<SyncMetadataEntity>>

    @Query("SELECT * FROM sync_metadata WHERE dataType = :type LIMIT 1")
    suspend fun getByType(type: String): SyncMetadataEntity?

    @Query("SELECT * FROM sync_metadata WHERE dataType = :type LIMIT 1")
    fun observeByType(type: String): Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadataEntity)

    @Query("UPDATE sync_metadata SET lastSyncedAt = :syncedAt, status = :status, recordCount = :count WHERE dataType = :type")
    suspend fun updateSync(type: String, syncedAt: Long, status: String, count: Int)
}

// ─────────────────────────────────────────────────────────────
// Payment Method DAO
// ─────────────────────────────────────────────────────────────

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY isPlaceholder ASC, name ASC")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE type = :type")
    fun observeByType(type: String): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE isActive = 1")
    fun observeActive(): Flow<List<PaymentMethodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(methods: List<PaymentMethodEntity>)
}
