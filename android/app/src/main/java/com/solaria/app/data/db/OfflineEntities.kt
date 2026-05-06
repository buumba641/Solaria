package com.solaria.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * All offline-first entities for the Solaria local database.
 * These tables are the single source of truth — the app reads from here,
 * and future backend services will write to these tables.
 */

// ─────────────────────────────────────────────────────────────
// Wallet
// ─────────────────────────────────────────────────────────────

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val address: String,
    val label: String = "Main Wallet",
    val balanceSol: Double = 0.0,
    val balanceLamports: Long = 0L,
    val isConnected: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "token_balances")
data class TokenBalanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletAddress: String,
    val mint: String,
    val symbol: String,
    val name: String = "",
    val amount: Double = 0.0,
    val usdValue: Double? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────
// Transactions (on-chain)
// ─────────────────────────────────────────────────────────────

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,           // UUID
    val fromAddress: String,
    val toAddress: String,
    val amountSol: Double,
    val amountLamports: Long = 0L,
    val feeSol: Double? = null,
    val type: String,                      // SEND, RECEIVE
    val status: String,                    // PENDING, CONFIRMED, FAILED
    val paymentMethod: String,             // DEVNET, MOBILE_MONEY, BANK
    val txHash: String? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val confirmedAt: Long? = null,
    val syncedAt: Long? = null,
    val isOffline: Boolean = false         // was created while offline
)

// ─────────────────────────────────────────────────────────────
// NFT Collections (cached from API, displayed offline)
// ─────────────────────────────────────────────────────────────

@Entity(tableName = "nft_collections")
data class NftCollectionEntity(
    @PrimaryKey val symbol: String,
    val name: String,
    val image: String? = null,
    val floorPrice: Double = 0.0,
    val volumeAll: Double? = null,
    val change24h: Double? = null,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

// ─────────────────────────────────────────────────────────────
// Sync Metadata — tracks when each data category was last updated
// ─────────────────────────────────────────────────────────────

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val dataType: String,      // wallets, prices, nfts, transactions, ai_data
    val lastSyncedAt: Long,
    val status: String = "never",          // synced, syncing, error, never
    val recordCount: Int = 0
)

// ─────────────────────────────────────────────────────────────
// Payment Methods (Devnet is real; bank/mobile money are placeholders)
// ─────────────────────────────────────────────────────────────

@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey val id: String,
    val type: String,                      // DEVNET, MOBILE_MONEY, BANK
    val name: String,
    val provider: String,                  // Solana, MTN, Airtel, Standard Bank
    val accountInfo: String? = null,       // masked account number
    val isActive: Boolean = true,
    val isPlaceholder: Boolean = false,    // true for bank/mobile money
    val iconType: String = "default"       // for UI icon selection
)
