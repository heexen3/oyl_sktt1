package cl.oyl.frutossecos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.oyl.frutossecos.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)

    @Upsert
    suspend fun upsert(product: ProductEntity)

    /** Catálogo filtrable: categoryId null = todas; query vacía = sin filtro de texto. */
    @Query(
        """SELECT * FROM products
           WHERE (:onlyActive = 0 OR active = 1)
             AND (:categoryId IS NULL OR categoryId = :categoryId)
             AND (:query = '' OR name LIKE '%' || :query || '%')
           ORDER BY name"""
    )
    fun observe(query: String = "", categoryId: Long? = null, onlyActive: Boolean = true): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE qrCode = :qrCode LIMIT 1")
    suspend fun findByQr(qrCode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE stock <= minStock ORDER BY stock")
    fun observeLowStock(): Flow<List<ProductEntity>>

    @Query("UPDATE products SET stock = :stock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: Long, stock: Int, updatedAt: Long)

    /** Descuento condicional: solo aplica si hay stock suficiente (devuelve filas afectadas). */
    @Query("UPDATE products SET stock = stock - :qty, updatedAt = :updatedAt WHERE id = :id AND stock >= :qty")
    suspend fun decrementIfAvailable(id: Long, qty: Int, updatedAt: Long): Int

    @Query("UPDATE products SET active = :active, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean, updatedAt: Long)

    @Query("SELECT COALESCE(MAX(id), 0) FROM products")
    suspend fun maxId(): Long

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
