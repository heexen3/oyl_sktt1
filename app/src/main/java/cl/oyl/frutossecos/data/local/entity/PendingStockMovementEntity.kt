package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.SyncStatus

/** Movimiento de inventario pendiente de enviar. [localId] (UUID) permite idempotencia en el backend. */
@Entity(
    tableName = "pending_stock_movements",
    foreignKeys = [ForeignKey(ProductEntity::class, ["id"], ["productId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("productId"), Index("syncStatus")],
)
data class PendingStockMovementEntity(
    @PrimaryKey val localId: String,
    val productId: Long,
    val type: MovementType,
    val quantity: Int,
    val createdAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDIENTE,
)
