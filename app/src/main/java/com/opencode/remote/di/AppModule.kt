package com.opencode.remote.di

import com.opencode.remote.data.api.AuthInterceptor
import com.opencode.remote.data.api.DynamicBaseUrlInterceptor
import com.opencode.remote.data.api.OpenCodeApi
import com.opencode.remote.data.local.SettingsDataStore
import com.opencode.remote.data.repository.OpenCodeRepository
import com.opencode.remote.data.repository.OpenCodeRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: android.content.Context
    ): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        dynamicBaseUrlInterceptor: DynamicBaseUrlInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://0.0.0.0/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenCodeApi(retrofit: Retrofit): OpenCodeApi {
        return retrofit.create(OpenCodeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenCodeRepository(
        api: OpenCodeApi,
        settingsDataStore: SettingsDataStore
    ): OpenCodeRepository {
        return OpenCodeRepositoryImpl(api, settingsDataStore)
    }
}
