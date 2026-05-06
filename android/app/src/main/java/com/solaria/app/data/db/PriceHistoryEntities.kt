package com.solaria.app.data.db

import androidx.room.Entity

/**
 * Rolling-window price history entities.
 * Each tier has a constant max row count per coin:
 *   hourly  → 168 rows (7 days)
 *   daily   → 365 rows (1 year)
 *   weekly  → 260 rows (5 years)
 *
 * Total across 22 coins × 3 tiers ≈ 17,400 rows — negligible.
 */

@Entity(
    tableName = "price_hourly",
    primaryKeys = ["coinId", "timestampMs"]
)
data class HourlyPriceEntity(
    val coinId: String,        // CoinGecko ID (e.g., "solana")
    val timestampMs: Long,     // Unix ms
    val priceUsd: Double
)

@Entity(
    tableName = "price_daily",
    primaryKeys = ["coinId", "timestampMs"]
)
data class DailyPriceEntity(
    val coinId: String,
    val timestampMs: Long,
    val priceUsd: Double
)

@Entity(
    tableName = "price_weekly",
    primaryKeys = ["coinId", "timestampMs"]
)
data class WeeklyPriceEntity(
    val coinId: String,
    val timestampMs: Long,
    val priceUsd: Double
)
