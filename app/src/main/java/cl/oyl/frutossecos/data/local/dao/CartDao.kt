package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.oyl.frutossecos.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

/** Fila del carrito con datos del producto. */
data class CartLineRow(
    val productId: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
    val stock: Int,
    val imageUrl: String?,
)

/** Todas las consultas filtran por userId: carritos aislados por cliente. */
@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    fun observeItems(userId: Long): Flow<List<CartItemEntity>>

    @Query(
        """SELECT c.productId AS productId, p.name AS name, p.price AS price, c.quantity AS quantity,
                  p.stock AS stock, COALESCE(p.imagePath, p.imageUrl) AS imageUrl
           FROM cart_items c INNER JOIN products p ON p.id = c.productId
           WHERE c.userId = :userId ORDER BY p.name"""
    )
    fun observeLines(userId: Long): Flow<List<CartLineRow>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun getItem(userId: Long, productId: Long): CartItemEntity?

    @Upsert
    suspend fun upsert(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE userId = :userId AND productId = :productId")
    suspend fun updateQuantity(userId: Long, productId: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND productId = :productId")
    suspend fun remove(userId: Long, productId: Long)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clear(userId: Long)

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE userId = :userId")
    fun observeCount(userId: Long): Flow<Int>

    @Query(
        """SELECT c.productId AS productId, p.name AS name, p.price AS price, c.quantity AS quantity,
                  p.stock AS stock, COALESCE(p.imagePath, p.imageUrl) AS imageUrl
           FROM cart_items c INNER JOIN products p ON p.id = c.productId
           WHERE c.userId = :userId ORDER BY p.name"""
    )
    suspend fun getLines(userId: Long): List<CartLineRow>
}
