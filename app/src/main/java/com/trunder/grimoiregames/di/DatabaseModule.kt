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
            // 👇 AÑADIMOS LA MIGRACIÓN DE V3 A V4
            // Esto ejecutará el script SQL que definimos en AppDatabase para añadir las columnas
            .addMigrations(AppDatabase.MIGRATION_3_4)

            // 👇 ¡IMPORTANTE! Comentamos esto.
            // Si lo dejas activado, si la migración falla o Room se lía, BORRARÁ toda la base de datos.
            // Al comentarlo, si algo falla, la app crasheará (avisándote) en lugar de borrar tus juegos.
            // .fallbackToDestructiveMigration()

            .build()
    }

    // 2. Enseñamos a Hilt cómo crear el DAO
    // (Necesita la base de datos que acabamos de enseñar arriba)
    @Provides
    fun provideGameDao(database: AppDatabase): GameDao {
        return database.gameDao()
    }
}