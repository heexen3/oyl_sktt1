package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.oyl.frutossecos.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM session WHERE id = 1")
    fun observe(): Flow<SessionEntity?>

    @Query("SELECT * FROM session WHERE id = 1")
    suspend fun get(): SessionEntity?

    @Upsert
    suspend fun save(session: SessionEntity)

    /** Cerrar sesión borra SOLO la sesión, nunca datos de otros usuarios. */
    @Query("DELETE FROM session")
    suspend fun clear()
}
