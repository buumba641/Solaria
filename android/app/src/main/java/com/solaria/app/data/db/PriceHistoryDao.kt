package com.solaria.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {

    // ── Hourly ──
    @Query("SELECT * FROM price_hourly WHERE coinId = :coinId ORDER BY timestampMs DESC")
    fun observeHourly(coinId: String): Flow<List<HourlyPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourly(price: HourlyPriceEntity)

    @Query("DELETE FROM price_hourly WHERE coinId = :coinId AND timestampMs NOT IN (SELECT timestampMs FROM price_hourly WHERE coinId = :coinId ORDER BY timestampMs DESC LIMIT :limit)")
    suspend fun trimHourly(coinId: String, limit: Int = 168)

    @Transaction
    suspend fun insertAndMaintainHourly(price: HourlyPriceEntity) {
        insertHourly(price)
        trimHourly(price.coinId, 168)
    }

    // ── Daily ──
    @Query("SELECT * FROM price_daily WHERE coinId = :coinId ORDER BY timestampMs DESC")
    fun observeDaily(coinId: String): Flow<List<DailyPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaily(price: DailyPriceEntity)

    @Query("DELETE FROM price_daily WHERE coinId = :coinId AND timestampMs NOT IN (SELECT timestampMs FROM price_daily WHERE coinId = :coinId ORDER BY timestampMs DESC LIMIT :limit)")
    suspend fun trimDaily(coinId: String, limit: Int = 365)

    @Transaction
    suspend fun insertAndMaintainDaily(price: DailyPriceEntity) {
        insertDaily(price)
        trimDaily(price.coinId, 365)
    }

    // ── Weekly ──
    @Query("SELECT * FROM price_weekly WHERE coinId = :coinId ORDER BY timestampMs DESC")
    fun observeWeekly(coinId: String): Flow<List<WeeklyPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeekly(price: WeeklyPriceEntity)

    @Query("DELETE FROM price_weekly WHERE coinId = :coinId AND timestampMs NOT IN (SELECT timestampMs FROM price_weekly WHERE coinId = :coinId ORDER BY timestampMs DESC LIMIT :limit)")
    suspend fun trimWeekly(coinId: String, limit: Int = 260)

    @Transaction
    suspend fun insertAndMaintainWeekly(price: WeeklyPriceEntity) {
        insertWeekly(price)
        trimWeekly(price.coinId, 260)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHourlyBatch(prices: List<HourlyPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyBatch(prices: List<DailyPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeeklyBatch(prices: List<WeeklyPriceEntity>)
}
