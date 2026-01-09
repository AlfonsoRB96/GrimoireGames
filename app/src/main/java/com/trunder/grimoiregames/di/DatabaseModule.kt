package com.trunder.grimoiregames.di

import android.content.Context
import androidx.room.Room
import com.trunder.grimoiregames.data.AppDatabase
import com.trunder.grimoiregames.data.dao.GameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Este módulo vive tanto como la App entera
object DatabaseModule {

    // 1. Enseñamos a Hilt cómo crear la BASE DE DATOS
    @Provides
    @Singleton // ¡IMPORTANTE! Solo queremos UNA instancia de la BBDD para toda la app
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "grimoire_database"
        )
            .fallbackToDestructiveMigration() // Si cambias la tabla, borra y empieza de 0 (útil en dev)
            .build()
    }

    // 2. Enseñamos a Hilt cómo crear el DAO
    // (Necesita la base de datos que acabamos de enseñar arriba)
    @Provides
    fun provideGameDao(database: AppDatabase): GameDao {
        return database.gameDao()
    }
}