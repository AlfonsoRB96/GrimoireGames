package com.trunder.grimoiregames.di

import com.trunder.grimoiregames.data.remote.RawgApi
import com.trunder.grimoiregames.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 1. Proveemos la instancia de Retrofit (El Cliente HTTP)
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Usamos Gson para traducir JSON
            .build()
    }

    // 2. Proveemos la API (La interfaz que creaste antes)
    @Provides
    @Singleton
    fun provideRawgApi(retrofit: Retrofit): RawgApi {
        return retrofit.create(RawgApi::class.java)
    }
}