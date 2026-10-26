package cl.oyl.frutossecos.data.repository

import cl.oyl.frutossecos.domain.logic.RegisterForm
import cl.oyl.frutossecos.domain.logic.StockIssue
import cl.oyl.frutossecos.domain.model.AppNotification
import cl.oyl.frutossecos.domain.model.CartLine
import cl.oyl.frutossecos.domain.model.Category
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.OrderItem
import cl.oyl.frutossecos.domain.model.OrderStatus
import cl.oyl.frutossecos.domain.model.Product
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.domain.model.Store
import cl.oyl.frutossecos.domain.model.User
import kotlinx.coroutines.flow.Flow

/*
 * Contratos de datos. En esta versión todas las implementaciones son Local (Room/SQLite);
 * las Remote (Retrofit + JWT contra el backend Spring Boot) se agregarán sin cambiar la UI.
 */

enum class AuthError { INVALID_CREDENTIALS, ALIAS_TAKEN, EMAIL_TAKEN }
class AuthException(val error: AuthError) : Exception(error.name)

/** Errores de negocio de pedidos e inventario. */
class StockException(val issues: List<StockIssue>) : Exception("Stock insuficiente")
class EmptyCartException : Exception("Carrito vacío")
class InvalidTransitionException : Exception("Transición de estado no permitida")

interface ProductRepository {
    fun observeCatalog(query: String = "", categoryId: Long? = null, onlyActive: Boolean = true): Flow<List<Product>>
    fun observeAll(): Flow<List<Product>>
    fun observeCategories(): Flow<List<Category>>
    fun observeProduct(id: Long): Flow<Product?>
    fun observeLowStock(): Flow<List<Product>>
    suspend fun findByQr(raw: String): Product?
    suspend fun save(draft: ProductDraft): Long
    suspend fun setActive(id: Long, active: Boolean)
}

data class ProductDraft(
    val id: Long? = null,
    val name: String,
    val categoryId: Long,
    val description: String,
    val price: Long,
    val presentation: String,
    val weightGrams: Int,
    val origin: String,
    val imagePath: String?,
    val stock: Int,
    val minStock: Int,
    val active: Boolean = true,
)

interface CartRepository {
    fun observeLines(userId: Long): Flow<List<CartLine>>
    fun observeCount(userId: Long): Flow<Int>
    /** Agrega [quantity] unidades respetando el stock. Devuelve la cantidad final en el carrito. */
    suspend fun add(userId: Long, productId: Long, quantity: Int = 1): Int
    suspend fun setQuantity(userId: Long, productId: Long, quantity: Int)
    suspend fun remove(userId: Long, productId: Long)
    suspend fun clear(userId: Long)
}

interface OrderRepository {
    /** Crea el pedido de forma transaccional: valida y descuenta stock, vacía el carrito y notifica. */
    suspend fun placeOrder(userId: Long, mode: DeliveryMode): Order
    fun observeOrders(userId: Long, role: Role, status: OrderStatus? = null): Flow<List<Order>>
    fun observeOrder(orderId: Long): Flow<Order?>
    fun observeItems(orderId: Long): Flow<List<OrderItem>>
    fun observePendingCount(): Flow<Int>
    suspend fun updateStatus(orderId: Long, status: OrderStatus)
}

interface InventoryRepository {
    /** Registra una entrada/salida. Devuelve el nuevo stock. */
    suspend fun adjust(productId: Long, type: MovementType, quantity: Int): Int
    fun observePendingMovements(): Flow<Int>
}

interface StoreRepository {
    fun observeStores(): Flow<List<Store>>
}

interface NotificationRepository {
    fun observe(userId: Long): Flow<List<AppNotification>>
    fun observeUnreadCount(userId: Long): Flow<Int>
    suspend fun markRead(userId: Long, id: Long)
    suspend fun markAllRead(userId: Long)
}

interface ProfileRepository {
    fun observeUser(userId: Long): Flow<User?>
    suspend fun update(user: User)
}
