package com.trunder.grimoiregames.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.trunder.grimoiregames.data.dao.GameDao
import com.trunder.grimoiregames.data.entity.Game

// 1. Definimos las tablas (entities) y la versión de la BBDD.
@Database(entities = [Game::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // 2. Exponemos los DAOs
    abstract fun gameDao(): GameDao

    // 3. Patrón Singleton (Para que solo exista una instancia de la BBDD)
    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Si la instancia ya existe, la devolvemos.
            // Si no, la creamos en un bloque sincronizado (Thread Safe).
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "grimoire_database" // Nombre del archivo físico en el móvil
                )
                    // WIPE DATA ON SCHEMA CHANGE: Si cambias la tabla, borra todo (útil para dev)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}