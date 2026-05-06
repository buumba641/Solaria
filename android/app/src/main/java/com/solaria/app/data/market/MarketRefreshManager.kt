package com.solaria.app.data.market

import com.solaria.app.data.db.SyncMetadataDao
import com.solaria.app.data.db.SyncMetadataEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketRefreshManager @Inject constructor(
    private val syncMetadataDao: SyncMetadataDao
) {

    suspend fun shouldRefresh(dataType: String): Boolean {
        val metadata = syncMetadataDao.getByType(dataType) ?: return true
        val now = System.currentTimeMillis()
        val diff = now - metadata.lastSyncedAt
        
        return when (dataType) {
            "hourly" -> diff >= 3_600_000
            "daily" -> diff >= 86_400_000
            "weekly" -> diff >= 604_800_000
            else -> diff >= 86_400_000
        }
    }

    suspend fun markRefreshed(dataType: String) {
        val now = System.currentTimeMillis()
        val existing = syncMetadataDao.getByType(dataType)
        if (existing != null) {
            syncMetadataDao.updateSync(dataType, now, "synced", existing.recordCount)
        } else {
            syncMetadataDao.upsert(SyncMetadataEntity(dataType, now, "synced", 0))
        }
    }
}
