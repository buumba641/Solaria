package com.solaria.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceCacheDao {
    @Query("SELECT * FROM price_cache ORDER BY symbol ASC")
    fun observeAll(): Flow<List<PriceCacheEntity>>

    @Query("SELECT * FROM price_cache WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): PriceCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PriceCacheEntity)

    @Query("DELETE FROM price_cache WHERE updatedAtMs < :olderThanMs")
    suspend fun evictStale(olderThanMs: Long)
}
