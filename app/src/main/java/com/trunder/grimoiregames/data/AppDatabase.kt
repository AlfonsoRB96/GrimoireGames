package com.trunder.grimoiregames.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game

// 1. Definimos las tablas (entities) y la versión de la BBDD.
@Database(entities = [Game::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // 2. Exponemos los DAOs
    abstract fun gameDao(): GameDao

    // 3. Patrón Singleton
    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Añadimos la columna 'franchise' de tipo TEXT que puede ser NULL
                database.execSQL("ALTER TABLE games ADD COLUMN franchise TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "grimoire_database"
                )
                    // 🔴 AÑADIMOS LA MIGRACIÓN
                    //.addMigrations(MIGRATION_3_4)


                    .fallbackToDestructiveMigration()

                    .build()
                    .also { Instance = it }
            }
        }
    }
}