package com.trunder.grimoiregames.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game

// 1. Definimos las tablas (entities) y la versión de la BBDD.
@Database(entities = [Game::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 2. Exponemos los DAOs
    abstract fun gameDao(): GameDao

    // 3. Patrón Singleton
    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        // 🔴 DEFINIMOS LA MIGRACIÓN DE V3 A V4
        val MIGRATION_3_4 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE games ADD COLUMN opencriticPress INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE games ADD COLUMN opencriticUser INTEGER DEFAULT NULL")
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