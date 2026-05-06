package com.solaria.app.data.api

import com.solaria.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Network module — kept for backend integration via Express (backend/src).
 * Currently the app runs offline-first using Room DB + SolariaRepository.
 * When the Express backend is configured, set useMock = false and the Retrofit
 * service will be used for real API calls.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val apiKeyInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-api-key", BuildConfig.API_KEY)
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideSolariaApiService(
        retrofit: Retrofit,
        mockService: MockSolariaApiService
    ): SolariaApiService {
        // Primary data flow: SolariaRepository → Room DB + SolanaRpcClient (direct DevNet).
        // This mock service is a fallback for any ViewModel referencing the Retrofit API directly.
        // Set to false to enable the Express backend (backend/src) via Retrofit.
        val useMock = true
        return if (useMock) mockService else retrofit.create(SolariaApiService::class.java)
    }
}
