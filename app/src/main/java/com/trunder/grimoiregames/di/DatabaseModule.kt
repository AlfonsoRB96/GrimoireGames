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
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "grimoire_database"
        )
            // 1. ❌ ELIMINA o comenta la migración vieja que da problemas
            // .addMigrations(AppDatabase.MIGRATION_3_4)

            // 2. ✅ DESCOMENTA esto para permitir borrar la BBDD si algo no cuadra
            .fallbackToDestructiveMigration()

            .build()
    }

    @Provides
    fun provideGameDao(database: AppDatabase): GameDao {
        return database.gameDao()
    }
}