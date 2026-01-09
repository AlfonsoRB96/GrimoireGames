package com.trunder.grimoiregames.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.trunder.grimoiregames.data.entity.Game
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // CONSULTA (SELECT)
    // Fíjate que devuelve un Flow<List<Game>>.
    // Flow es como un "Stream" de datos en tiempo real.
    // Si añades un juego, la UI se actualizará SOLA automáticamente. ¡Magia pura!
    @Query("SELECT * FROM games ORDER BY title ASC")
    fun getAllGames(): Flow<List<Game>>

    // BUSCAR POR ID
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Int): Game?

    // INSERTAR O ACTUALIZAR (UPSERT)
    // onConflict = REPLACE: Si intentas meter un juego con el mismo ID, lo sobrescribe.
    // 'suspend' significa que esta función se ejecuta en segundo plano (Coroutines)
    // para no congelar la pantalla.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game)

    // BORRAR
    @Delete
    suspend fun deleteGame(game: Game)
}