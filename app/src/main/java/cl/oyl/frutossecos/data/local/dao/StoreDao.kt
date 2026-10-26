package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.oyl.frutossecos.data.local.entity.StoreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Upsert
    suspend fun upsertAll(stores: List<StoreEntity>)

    @Query("SELECT * FROM stores ORDER BY type, name")
    fun observeAll(): Flow<List<StoreEntity>>

    @Query("SELECT COUNT(*) FROM stores")
    suspend fun count(): Int
}
