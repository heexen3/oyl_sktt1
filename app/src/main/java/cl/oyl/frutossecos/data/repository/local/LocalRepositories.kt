package cl.oyl.frutossecos.data.repository.local

import androidx.room.withTransaction
import cl.oyl.frutossecos.data.local.OylDatabase
import cl.oyl.frutossecos.data.local.entity.CartItemEntity
import cl.oyl.frutossecos.data.local.entity.OrderEntity
import cl.oyl.frutossecos.data.local.entity.OrderItemEntity
import cl.oyl.frutossecos.data.local.entity.PendingStockMovementEntity
import cl.oyl.frutossecos.data.local.entity.ProductEntity
import cl.oyl.frutossecos.data.local.mapper.toDomain
import cl.oyl.frutossecos.data.local.seed.SeedData
import cl.oyl.frutossecos.data.repository.CartRepository
import cl.oyl.frutossecos.data.repository.EmptyCartException
import cl.oyl.frutossecos.data.repository.InventoryRepository
import cl.oyl.frutossecos.data.repository.InvalidTransitionException
import cl.oyl.frutossecos.data.repository.NotificationRepository
import cl.oyl.frutossecos.data.repository.OrderRepository
import cl.oyl.frutossecos.data.repository.ProductDraft
import cl.oyl.frutossecos.data.repository.ProductRepository
import cl.oyl.frutossecos.data.repository.ProfileRepository
import cl.oyl.frutossecos.data.repository.StockException
import cl.oyl.frutossecos.data.repository.StoreRepository
import cl.oyl.frutossecos.domain.logic.CartRules
import cl.oyl.frutossecos.domain.logic.OrderRules
import cl.oyl.frutossecos.domain.logic.QrParser
import cl.oyl.frutossecos.domain.logic.StockIssue
import cl.oyl.frutossecos.domain.model.AppNotification
import cl.oyl.frutossecos.domain.model.CartLine
import cl.oyl.frutossecos.domain.model.Category
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.NotificationType
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.OrderItem
import cl.oyl.frutossecos.domain.model.OrderStatus
import cl.oyl.frutossecos.domain.model.Product
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.domain.model.Store
import cl.oyl.frutossecos.domain.model.User
import cl.oyl.frutossecos.domain.model.SyncStatus
import cl.oyl.frutossecos.core.util.CurrencyFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private fun now() = System.currentTimeMillis()

@Singleton
class LocalProductRepository @Inject constructor(private val db: OylDatabase) : ProductRepository {
    private val dao = db.productDao()

    override fun observeCatalog(query: String, categoryId: Long?, onlyActive: Boolean): Flow<List<Product>> =
        dao.observe(query.trim(), categoryId, onlyActive).map { list -> list.map { it.toDomain() } }

    override fun observeAll(): Flow<List<Product>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        db.categoryDao().observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeProduct(id: Long): Flow<Product?> = dao.observeById(id).map { it?.toDomain() }

    override fun observeLowStock(): Flow<List<Product>> = dao.observeLowStock().map { list -> list.map { it.toDomain() } }

    override suspend fun findByQr(raw: String): Product? {
        val code = QrParser.normalize(raw) ?: return null
        return dao.findByQr(code)?.toDomain()
    }

    override suspend fun save(draft: ProductDraft): Long = db.withTransaction {
        val existing = draft.id?.let { dao.getById(it) }
        val id = existing?.id ?: (dao.maxId() + 1)
        dao.upsert(
            ProductEntity(
                id = id, name = draft.name.trim(), categoryId = draft.categoryId, description = draft.description.trim(),
                price = draft.price, presentation = draft.presentation.trim(), weightGrams = draft.weightGrams,
                origin = draft.origin.trim(), imageUrl = existing?.imageUrl, imagePath = draft.imagePath ?: existing?.imagePath,
                stock = draft.stock, minStock = draft.minStock, active = draft.active,
                qrCode = existing?.qrCode ?: SeedData.qrFor(id), updatedAt = now(),
            )
        )
        id
    }

    override suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active, now())
}

@Singleton
class LocalCartRepository @Inject constructor(private val db: OylDatabase) : CartRepository {
    private val dao = db.cartDao()

    override fun observeLines(userId: Long): Flow<List<CartLine>> = dao.observeLines(userId).map { l -> l.map { it.toDomain() } }
    override fun observeCount(userId: Long): Flow<Int> = dao.observeCount(userId)

    override suspend fun add(userId: Long, productId: Long, quantity: Int): Int = db.withTransaction {
        val product = db.productDao().getById(productId) ?: return@withTransaction 0
        val current = dao.getItem(userId, productId)?.quantity ?: 0
        val target = CartRules.clamp(current + quantity, product.stock)
        if (target <= 0) dao.remove(userId, productId)
        else dao.upsert(CartItemEntity(userId = userId, productId = productId, quantity = target))
        target
    }

    override suspend fun setQuantity(userId: Long, productId: Long, quantity: Int) {
        if (quantity <= 0) dao.remove(userId, productId) else dao.updateQuantity(userId, productId, quantity)
    }

    override suspend fun remove(userId: Long, productId: Long) = dao.remove(userId, productId)
    override suspend fun clear(userId: Long) = dao.clear(userId)
}

@Singleton
class LocalOrderRepository @Inject constructor(
    private val db: OylDatabase,
    private val publisher: NotificationPublisher,
) : OrderRepository {
    private val dao = db.orderDao()

    override suspend fun placeOrder(userId: Long, mode: DeliveryMode): Order {
        val notifications = mutableListOf<NotificationPublisher.Pending>()
        val order = db.withTransaction {
            val lines = db.cartDao().getLines(userId).map { it.toDomain() }
            if (lines.isEmpty()) throw EmptyCartException()
            CartRules.issues(lines).takeIf { it.isNotEmpty() }?.let { throw StockException(it) }
            val ts = now()
            lines.forEach { line ->
                // Descuento condicional: si otro pedido consumió el stock, se revierte toda la transacción.
                if (db.productDao().decrementIfAvailable(line.productId, line.quantity, ts) == 0) {
                    val available = db.productDao().getById(line.productId)?.stock ?: 0
                    throw StockException(listOf(StockIssue(line.productId, line.name, line.quantity, available)))
                }
                val p = db.productDao().getById(line.productId)!!
                if (p.stock <= p.minStock) {
                    notifications += publisher.toAdmins(NotificationType.STOCK_BAJO, "Stock bajo: ${p.name}",
                        "Quedan ${p.stock} unidades (mínimo ${p.minStock}).", "admin/inventory", p.id)
                }
            }
            val subtotal = OrderRules.subtotal(lines)
            val shipping = OrderRules.shipping(subtotal, mode)
            val id = dao.maxId() + 1
            val entity = OrderEntity(id, userId, OrderRules.orderNumber(id), OrderStatus.PENDIENTE_VALIDACION,
                subtotal + shipping, shipping, mode, ts)
            dao.insertOrderWithItems(entity, lines.map { OrderItemEntity(id, it.productId, it.name, it.unitPrice, it.quantity) })
            db.cartDao().clear(userId)
            notifications += publisher.toAdmins(NotificationType.NUEVO_PEDIDO, "Nuevo pedido ${entity.number}",
                "Total ${CurrencyFormatter.clp(entity.total)} · pendiente de validación.", "orders", id)
            notifications += publisher.build(userId, NotificationType.CAMBIO_ESTADO, "Pedido ${entity.number} recibido",
                "Tu pedido quedó pendiente de validación.", "orders", id)
            entity
        }
        publisher.alertIfCurrent(publisher.insert(notifications))
        return order.toDomain()
    }

    override fun observeOrders(userId: Long, role: Role, status: OrderStatus?): Flow<List<Order>> =
        (if (role == Role.ADMINISTRADOR) dao.observeAll(status) else dao.observeForUser(userId, status))
            .map { l -> l.map { it.toDomain() } }

    override fun observeOrder(orderId: Long): Flow<Order?> = dao.observeById(orderId).map { it?.toDomain() }
    override fun observeItems(orderId: Long): Flow<List<OrderItem>> = dao.observeItems(orderId).map { l -> l.map { it.toDomain() } }
    override fun observePendingCount(): Flow<Int> = dao.observeCountByStatus(OrderStatus.PENDIENTE_VALIDACION)

    override suspend fun updateStatus(orderId: Long, status: OrderStatus) {
        val pending = db.withTransaction {
            val order = dao.getById(orderId) ?: throw InvalidTransitionException()
            if (!OrderRules.canTransition(order.status, status)) throw InvalidTransitionException()
            dao.updateStatus(orderId, status)
            val restock = mutableListOf<NotificationPublisher.Pending>()
            if (status == OrderStatus.CANCELADO) {
                // Devuelve el stock reservado por el pedido cancelado.
                dao.getItems(orderId).forEach { item ->
                    db.productDao().getById(item.productId)?.let { p ->
                        db.productDao().updateStock(p.id, p.stock + item.quantity, now())
                    }
                }
            }
            restock + publisher.build(order.userId, NotificationType.CAMBIO_ESTADO, "Pedido ${order.number}",
                "Nuevo estado: ${statusLabel(status)}.", "orders", orderId)
        }
        publisher.alertIfCurrent(publisher.insert(pending))
    }

    private fun statusLabel(s: OrderStatus) = when (s) {
        OrderStatus.PENDIENTE_VALIDACION -> "Pendiente de validación"
        OrderStatus.CONFIRMADO -> "Confirmado"
        OrderStatus.PREPARADO -> "Preparado"
        OrderStatus.ENTREGADO -> "Entregado"
        OrderStatus.CANCELADO -> "Cancelado"
    }
}

@Singleton
class LocalInventoryRepository @Inject constructor(
    private val db: OylDatabase,
    private val publisher: NotificationPublisher,
) : InventoryRepository {
    override suspend fun adjust(productId: Long, type: MovementType, quantity: Int): Int {
        require(quantity > 0) { "La cantidad debe ser mayor a 0" }
        val notifications = mutableListOf<NotificationPublisher.Pending>()
        val newStock = db.withTransaction {
            val p = db.productDao().getById(productId) ?: throw IllegalArgumentException("Producto no existe")
            val stock = if (type == MovementType.ENTRADA) p.stock + quantity else {
                if (quantity > p.stock) throw StockException(listOf(StockIssue(p.id, p.name, quantity, p.stock)))
                p.stock - quantity
            }
            val ts = now()
            db.productDao().updateStock(p.id, stock, ts)
            // Cola de movimientos con localId (UUID) para sincronizar con el backend en el futuro.
            db.pendingStockMovementDao().insert(PendingStockMovementEntity(UUID.randomUUID().toString(), p.id, type, quantity, ts, SyncStatus.PENDIENTE))
            if (stock <= p.minStock && p.stock > p.minStock) {
                notifications += publisher.toAdmins(NotificationType.STOCK_BAJO, "Stock bajo: ${p.name}",
                    "Quedan $stock unidades (mínimo ${p.minStock}).", "admin/inventory", p.id)
            }
            stock
        }
        publisher.alertIfCurrent(publisher.insert(notifications))
        return newStock
    }

    override fun observePendingMovements(): Flow<Int> =
        db.pendingStockMovementDao().observeAll().map { l -> l.count { it.syncStatus != SyncStatus.SINCRONIZADO } }
}

@Singleton
class LocalStoreRepository @Inject constructor(private val db: OylDatabase) : StoreRepository {
    override fun observeStores(): Flow<List<Store>> = db.storeDao().observeAll().map { l -> l.map { it.toDomain() } }
}

@Singleton
class LocalNotificationRepository @Inject constructor(private val db: OylDatabase) : NotificationRepository {
    private val dao = db.notificationDao()
    override fun observe(userId: Long): Flow<List<AppNotification>> = dao.observeForUser(userId).map { l -> l.map { it.toDomain() } }
    override fun observeUnreadCount(userId: Long): Flow<Int> = dao.observeUnreadCount(userId)
    override suspend fun markRead(userId: Long, id: Long) = dao.markRead(userId, id)
    override suspend fun markAllRead(userId: Long) = dao.markAllRead(userId)
}

@Singleton
class LocalProfileRepository @Inject constructor(private val db: OylDatabase) : ProfileRepository {
    override fun observeUser(userId: Long): Flow<User?> = db.userDao().observeById(userId).map { it?.toDomain() }

    override suspend fun update(user: User) {
        val current = db.userDao().getById(user.id) ?: return
        db.userDao().update(
            current.copy(
                name = user.name.trim(), phone = user.phone.trim(), address = user.address?.trim()?.ifBlank { null },
                avatarId = user.avatarId, avatarUri = user.avatarUri, notificationsEnabled = user.notificationsEnabled,
            )
        )
    }
}
