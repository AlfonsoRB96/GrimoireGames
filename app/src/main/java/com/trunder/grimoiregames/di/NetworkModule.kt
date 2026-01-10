package com.trunder.grimoiregames.di

import com.trunder.grimoiregames.data.remote.RawgApi
import com.trunder.grimoiregames.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 1. Proveemos el cliente HTTP (Seguro, pero con Logs)
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // El interceptor nos permite ver el JSON en el Logcat (útil para debug)
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    // 2. Proveemos la API usando Retrofit + Gson
    @Provides
    @Singleton
    fun provideRawgApi(okHttpClient: OkHttpClient): RawgApi {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RawgApi::class.java)
    }
}