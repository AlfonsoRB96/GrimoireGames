package com.trunder.grimoiregames.di

import com.trunder.grimoiregames.data.remote.IgdbApi      // 👈 Nuevo
import com.trunder.grimoiregames.data.remote.TwitchAuthApi // 👈 Nuevo
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

    // 1. Proveemos el cliente HTTP (Compartido: Seguro y con Logs)
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

    // =================================================================
    // 👇 NUEVA IMPLEMENTACIÓN (IGDB + TWITCH)
    // =================================================================

    // 2. Proveemos la API de Autenticación (Para pedir el Token a Twitch)
    @Provides
    @Singleton
    fun provideTwitchAuthApi(okHttpClient: OkHttpClient): TwitchAuthApi {
        return Retrofit.Builder()
            .baseUrl(Constants.TWITCH_AUTH_URL)
            .client(okHttpClient) // Reusamos el cliente con logs
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwitchAuthApi::class.java)
    }

    // 3. Proveemos la API de Datos (IGDB)
    @Provides
    @Singleton
    fun provideIgdbApi(okHttpClient: OkHttpClient): IgdbApi {
        return Retrofit.Builder()
            .baseUrl(Constants.IGDB_BASE_URL)
            .client(okHttpClient) // Reusamos el cliente con logs
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IgdbApi::class.java)
    }

    // =================================================================
    // 👇 LEGACY / ANTIGUO (RAWG)
    // (Mantenido por si queremos volver a usarlo en el futuro)
    // =================================================================

    /*@Provides
    @Singleton
    fun provideRawgApi(okHttpClient: OkHttpClient): RawgApi {
        return Retrofit.Builder()
            // Asegúrate de que Constants.BASE_URL sigue existiendo o cámbialo por "https://api.rawg.io/api/"
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RawgApi::class.java)
    }*/
}