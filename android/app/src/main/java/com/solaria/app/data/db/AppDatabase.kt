package com.solaria.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        ChatMessageEntity::class,
        PriceCacheEntity::class,
        WalletEntity::class,
        TokenBalanceEntity::class,
        TransactionEntity::class,
        NftCollectionEntity::class,
        SyncMetadataEntity::class,
        PaymentMethodEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun priceCacheDao(): PriceCacheDao
    abstract fun walletDao(): WalletDao
    abstract fun tokenBalanceDao(): TokenBalanceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun nftCollectionDao(): NftCollectionDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun paymentMethodDao(): PaymentMethodDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        lateinit var db: AppDatabase
        db = Room.databaseBuilder(context, AppDatabase::class.java, "solaria.db")
            .fallbackToDestructiveMigration()
            .addCallback(SeedData { db })
            .build()
        return db
    }

    @Provides fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()
    @Provides fun providePriceCacheDao(db: AppDatabase): PriceCacheDao = db.priceCacheDao()
    @Provides fun provideWalletDao(db: AppDatabase): WalletDao = db.walletDao()
    @Provides fun provideTokenBalanceDao(db: AppDatabase): TokenBalanceDao = db.tokenBalanceDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideNftCollectionDao(db: AppDatabase): NftCollectionDao = db.nftCollectionDao()
    @Provides fun provideSyncMetadataDao(db: AppDatabase): SyncMetadataDao = db.syncMetadataDao()
    @Provides fun providePaymentMethodDao(db: AppDatabase): PaymentMethodDao = db.paymentMethodDao()
}
