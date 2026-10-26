package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "order_items",
    primaryKeys = ["orderId", "productId"],
    foreignKeys = [ForeignKey(OrderEntity::class, ["id"], ["orderId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("orderId"), Index("productId")],
)
data class OrderItemEntity(
    val orderId: Long,
    val productId: Long,
    val name: String,
    val unitPrice: Long,
    val quantity: Int,
)
