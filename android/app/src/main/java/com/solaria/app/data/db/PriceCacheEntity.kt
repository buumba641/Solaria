package com.solaria.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached token price data – mirrors how Crypto-KMP caches market data locally.
 */
@Entity(tableName = "price_cache")
data class PriceCacheEntity(
    @PrimaryKey val symbol: String,
    val price: Double,
    val change24h: Double,
    val volume24h: Double?,
    val marketCap: Double?,
    val historyJson: String?,   // JSON array of PricePoint
    val updatedAtMs: Long = System.currentTimeMillis()
)
