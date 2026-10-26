package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.OrderStatus

/** Caché local de pedidos (fuente oficial: backend). */
@Entity(
    tableName = "orders",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId"), Index(value = ["number"], unique = true)],
)
data class OrderEntity(
    @PrimaryKey val id: Long,
    val userId: Long,
    val number: String,
    val status: OrderStatus,
    val total: Long,
    val shipping: Long,
    val deliveryMode: DeliveryMode,
    val createdAt: Long,
    val syncedAt: Long? = null,
)
