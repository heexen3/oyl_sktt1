package cl.oyl.frutossecos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Carrito por cliente: PK compuesta (userId, productId). */
@Entity(
    tableName = "cart_items",
    primaryKeys = ["userId", "productId"],
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ProductEntity::class, ["id"], ["productId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("userId"), Index("productId")],
)
data class CartItemEntity(
    val userId: Long,
    val productId: Long,
    val quantity: Int,
)
