package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.oyl.frutossecos.data.local.entity.PendingStockMovementEntity
import cl.oyl.frutossecos.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingStockMovementDao {
    /** IGNORE: el mismo localId no se registra dos veces (idempotencia). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(movement: PendingStockMovementEntity): Long

    @Query("SELECT * FROM pending_stock_movements WHERE syncStatus != 'SINCRONIZADO' ORDER BY createdAt")
    suspend fun getPending(): List<PendingStockMovementEntity>

    @Query("SELECT * FROM pending_stock_movements ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PendingStockMovementEntity>>

    @Query("UPDATE pending_stock_movements SET syncStatus = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: SyncStatus)
}
