package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cl.oyl.frutossecos.data.local.entity.OrderEntity
import cl.oyl.frutossecos.data.local.entity.OrderItemEntity
import cl.oyl.frutossecos.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Upsert
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun insertOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        upsertOrder(order)
        insertItems(items)
    }

    /** Pedidos de un cliente (aislado por userId). status null = todos. */
    @Query("SELECT * FROM orders WHERE userId = :userId AND (:status IS NULL OR status = :status) ORDER BY createdAt DESC")
    fun observeForUser(userId: Long, status: OrderStatus? = null): Flow<List<OrderEntity>>

    /** Vista de Administrador: todos los pedidos. */
    @Query("SELECT * FROM orders WHERE (:status IS NULL OR status = :status) ORDER BY createdAt DESC")
    fun observeAll(status: OrderStatus? = null): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun observeById(orderId: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getById(orderId: Long): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun observeItems(orderId: Long): Flow<List<OrderItemEntity>>

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateStatus(orderId: Long, status: OrderStatus)

    @Query("SELECT COALESCE(MAX(id), 0) FROM orders")
    suspend fun maxId(): Long

    @Query("SELECT COUNT(*) FROM orders WHERE status = :status")
    fun observeCountByStatus(status: OrderStatus): Flow<Int>
}
