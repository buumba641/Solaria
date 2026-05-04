package com.solaria.app.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Populates the Room database with realistic seed data on first creation.
 * This lets the app run fully offline without any backend connection.
 *
 * When a backend (FastAPI, wallet server, etc.) is connected later,
 * it will write directly to these same tables in real-time.
 */
class SeedData(private val dbProvider: () -> AppDatabase) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            val database = dbProvider()
            seedWallets(database)
            seedTokenBalances(database)
            seedPriceCache(database)
            seedNftCollections(database)
            seedTransactions(database)
            seedPaymentMethods(database)
            seedSyncMetadata(database)
            seedChatMessages(database)
        }
    }

    private suspend fun seedWallets(db: AppDatabase) {
        db.walletDao().upsert(
            WalletEntity(
                address = "6a7bK9p2R5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h2f9c",
                label = "Main Wallet",
                balanceSol = 3.4826,
                balanceLamports = 3_482_600_000L,
                isConnected = true,
                lastSyncedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun seedTokenBalances(db: AppDatabase) {
        val wallet = "6a7bK9p2R5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h2f9c"
        db.tokenBalanceDao().upsertAll(
            listOf(
                TokenBalanceEntity(
                    id = 1, walletAddress = wallet,
                    mint = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
                    symbol = "USDC", name = "USD Coin",
                    amount = 247.50, usdValue = 247.50
                ),
                TokenBalanceEntity(
                    id = 2, walletAddress = wallet,
                    mint = "Es9vMFrzaDCSTv4asS98TM2dkMvBDSECPpOGv1ta169",
                    symbol = "USDT", name = "Tether",
                    amount = 85.00, usdValue = 85.00
                ),
                TokenBalanceEntity(
                    id = 3, walletAddress = wallet,
                    mint = "DezXAZ8z7PnrnRJjz3wXBoRgixCa6xjnB7YaB1pPB263",
                    symbol = "BONK", name = "Bonk",
                    amount = 4_500_000.0, usdValue = 112.50
                )
            )
        )
    }

    private suspend fun seedPriceCache(db: AppDatabase) {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L

        // SOL with 7-day history
        val solHistory = (0..167).map { i ->
            """{"timestamp":${now - (167 - i) * hour},"price":${138.0 + Math.sin(i * 0.15) * 8 + i * 0.06}}"""
        }
        db.priceCacheDao().upsert(
            PriceCacheEntity(
                symbol = "SOL", price = 148.24, change24h = 5.24,
                volume24h = 2_840_000_000.0, marketCap = 68_200_000_000.0,
                historyJson = "[${solHistory.joinToString(",")}]"
            )
        )

        // Additional tokens for market display
        db.priceCacheDao().upsert(
            PriceCacheEntity(
                symbol = "BONK", price = 0.000025, change24h = -2.1,
                volume24h = 180_000_000.0, marketCap = 1_560_000_000.0, historyJson = null
            )
        )
        db.priceCacheDao().upsert(
            PriceCacheEntity(
                symbol = "JUP", price = 0.82, change24h = 3.7,
                volume24h = 95_000_000.0, marketCap = 1_100_000_000.0, historyJson = null
            )
        )
    }

    private suspend fun seedNftCollections(db: AppDatabase) {
        val now = System.currentTimeMillis()
        db.nftCollectionDao().upsertAll(
            listOf(
                NftCollectionEntity("mad_lads", "Mad Lads", null, 120.0, 980_000.0, 4.2, now),
                NftCollectionEntity("tensorians", "Tensorians", null, 45.0, 320_000.0, -1.8, now),
                NftCollectionEntity("claynosaurz", "Claynosaurz", null, 32.0, 290_000.0, 2.5, now),
                NftCollectionEntity("famous_fox", "Famous Fox Federation", null, 18.0, 150_000.0, 0.9, now),
                NftCollectionEntity("okay_bears", "Okay Bears", null, 12.0, 140_000.0, -3.1, now),
                NftCollectionEntity("degods", "DeGods", null, 8.5, 120_000.0, 1.2, now)
            )
        )
    }

    private suspend fun seedTransactions(db: AppDatabase) {
        val now = System.currentTimeMillis()
        val wallet = "6a7bK9p2R5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h2f9c"
        db.transactionDao().apply {
            upsert(TransactionEntity(
                id = UUID.randomUUID().toString(), fromAddress = wallet,
                toAddress = "9WzDXwB3h7iV4d6m9uX6k6v6x6y6z6A", amountSol = 0.5,
                type = "SEND", status = "CONFIRMED", paymentMethod = "DEVNET",
                txHash = "5Gz...mock1", description = "Supplier payout",
                createdAt = now - 7_200_000, confirmedAt = now - 7_190_000, syncedAt = now - 7_190_000
            ))
            upsert(TransactionEntity(
                id = UUID.randomUUID().toString(),
                fromAddress = "3KfT8vR2X5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h",
                toAddress = wallet, amountSol = 1.2,
                type = "RECEIVE", status = "CONFIRMED", paymentMethod = "DEVNET",
                txHash = "8Bx...mock2", description = "Client payment",
                createdAt = now - 14_400_000, confirmedAt = now - 14_390_000, syncedAt = now - 14_390_000
            ))
            upsert(TransactionEntity(
                id = UUID.randomUUID().toString(), fromAddress = wallet,
                toAddress = "7PmN5vR2X5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h",
                amountSol = 0.05, type = "SEND", status = "CONFIRMED",
                paymentMethod = "MOBILE_MONEY", txHash = null,
                description = "MTN Mobile Money top-up",
                createdAt = now - 86_400_000, confirmedAt = now - 86_380_000, syncedAt = now - 86_380_000
            ))
            upsert(TransactionEntity(
                id = UUID.randomUUID().toString(),
                fromAddress = "2RsT5vR2X5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h",
                toAddress = wallet, amountSol = 0.8,
                type = "RECEIVE", status = "CONFIRMED", paymentMethod = "DEVNET",
                txHash = "4Hy...mock3", description = "USSD payment received",
                createdAt = now - 172_800_000, confirmedAt = now - 172_790_000, syncedAt = now - 172_790_000
            ))
            // A P2P transaction waiting to sync
            upsert(TransactionEntity(
                id = UUID.randomUUID().toString(), fromAddress = wallet,
                toAddress = "5JkL9vR2X5qVz8Y1v7Kz9y9vX3w4M8n6J2f9cT5h",
                amountSol = 0.15, type = "P2P_SEND", status = "PENDING_SYNC",
                paymentMethod = "P2P", txHash = null,
                description = "Nearby vendor payment (P2P)",
                createdAt = now - 3_600_000, isOffline = true
            ))
        }
    }

    private suspend fun seedPaymentMethods(db: AppDatabase) {
        db.paymentMethodDao().upsertAll(
            listOf(
                PaymentMethodEntity(
                    id = "devnet", type = "DEVNET", name = "Solana Devnet",
                    provider = "Solana", accountInfo = "Devnet Cluster",
                    isActive = true, isPlaceholder = false, iconType = "solana"
                ),
                PaymentMethodEntity(
                    id = "mtn_momo", type = "MOBILE_MONEY", name = "MTN Mobile Money",
                    provider = "MTN", accountInfo = "****7823",
                    isActive = true, isPlaceholder = true, iconType = "mobile_money"
                ),
                PaymentMethodEntity(
                    id = "airtel_money", type = "MOBILE_MONEY", name = "Airtel Money",
                    provider = "Airtel", accountInfo = "****4591",
                    isActive = true, isPlaceholder = true, iconType = "mobile_money"
                ),
                PaymentMethodEntity(
                    id = "standard_bank", type = "BANK", name = "Standard Bank",
                    provider = "Standard Bank", accountInfo = "****3847",
                    isActive = true, isPlaceholder = true, iconType = "bank"
                ),
                PaymentMethodEntity(
                    id = "fnb", type = "BANK", name = "First National Bank",
                    provider = "FNB", accountInfo = "****9012",
                    isActive = false, isPlaceholder = true, iconType = "bank"
                )
            )
        )
    }

    private suspend fun seedSyncMetadata(db: AppDatabase) {
        val now = System.currentTimeMillis()
        db.syncMetadataDao().apply {
            upsert(SyncMetadataEntity("wallets", now, "synced", 1))
            upsert(SyncMetadataEntity("prices", now, "synced", 3))
            upsert(SyncMetadataEntity("nfts", now, "synced", 6))
            upsert(SyncMetadataEntity("transactions", now, "synced", 5))
            upsert(SyncMetadataEntity("ai_data", now, "synced", 1))
        }
    }

    private suspend fun seedChatMessages(db: AppDatabase) {
        db.chatDao().insert(
            ChatMessageEntity(
                conversationId = "solaria_main",
                role = "bot",
                text = "👋 Welcome to Solaria! I'm your offline-first Solana AI assistant. Ask me about your balance, token prices, or say \"Send 0.1 SOL to…\" to start a transaction."
            )
        )
    }
}
