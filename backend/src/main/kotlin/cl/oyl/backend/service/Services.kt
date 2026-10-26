package cl.oyl.backend.service

import cl.oyl.backend.domain.CustomerOrder
import cl.oyl.backend.domain.DeliveryMode
import cl.oyl.backend.domain.DeviceToken
import cl.oyl.backend.domain.MovementType
import cl.oyl.backend.domain.Notification
import cl.oyl.backend.domain.NotificationType
import cl.oyl.backend.domain.OrderItem
import cl.oyl.backend.domain.OrderStatus
import cl.oyl.backend.domain.Product
import cl.oyl.backend.domain.Role
import cl.oyl.backend.domain.StockMovement
import cl.oyl.backend.domain.UserAccount
import cl.oyl.backend.repo.CategoryRepository
import cl.oyl.backend.repo.DeviceTokenRepository
import cl.oyl.backend.repo.NotificationRepository
import cl.oyl.backend.repo.OrderRepository
import cl.oyl.backend.repo.ProductRepository
import cl.oyl.backend.repo.StockMovementRepository
import cl.oyl.backend.repo.UserRepository
import cl.oyl.backend.security.CurrentUser
import cl.oyl.backend.security.TokenService
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// ---------- Reglas compartidas con la app ----------
object OrderRules {
    const val SHIPPING_FEE = 2_500L
    const val FREE_SHIPPING_FROM = 25_000L
    fun shipping(subtotal: Long, mode: DeliveryMode) =
        if (mode == DeliveryMode.RETIRO || subtotal >= FREE_SHIPPING_FROM) 0L else SHIPPING_FEE
    fun number(id: Long) = "OYL-%06d".format(id)
    fun canTransition(from: OrderStatus, to: OrderStatus) = when (to) {
        OrderStatus.CANCELADO -> from == OrderStatus.PENDIENTE_VALIDACION || from == OrderStatus.CONFIRMADO
        else -> from != OrderStatus.CANCELADO && from != OrderStatus.ENTREGADO && to.ordinal == from.ordinal + 1
    }
}

data class Line(val productId: Long, val quantity: Int)

// ---------- Autenticación ----------
data class AuthResult(val token: String, val user: UserAccount)

@Service
class AuthService(
    private val users: UserRepository,
    private val encoder: PasswordEncoder,
    private val tokens: TokenService,
) {
    @Transactional
    fun register(name: String, alias: String, email: String, password: String, phone: String, address: String?, avatarId: Int?): AuthResult {
        val a = alias.trim().lowercase()
        val e = email.trim().lowercase()
        if (users.existsByAliasIgnoreCase(a)) throw ApiException(HttpStatus.CONFLICT, "ALIAS_TAKEN", "El alias ya está en uso")
        if (users.existsByEmailIgnoreCase(e)) throw ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "El correo ya está registrado")
        val user = users.save(UserAccount(name.trim(), a, e, phone.trim(), address?.trim()?.ifBlank { null }, avatarId, encoder.encode(password)!!, Role.CLIENTE))
        return AuthResult(tokens.issue(user), user)
    }

    /** Login con alias o correo + contraseña + rol. Mensaje genérico ante cualquier error. */
    @Transactional(readOnly = true)
    fun login(login: String, password: String, role: Role): AuthResult {
        val l = login.trim()
        val user = (if ("@" in l) users.findByEmailIgnoreCase(l) else users.findByAliasIgnoreCase(l))
            ?.takeIf { it.role == role && encoder.matches(password, it.passwordHash) }
            ?: throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Credenciales inválidas")
        return AuthResult(tokens.issue(user), user)
    }
}

// ---------- Notificaciones / push ----------
/** Envío push. La implementación real usará Firebase Admin SDK (requiere credenciales de servicio, fuera del repo). */
interface PushSender { fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>) }

@Component
class LoggingPushSender : PushSender {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun send(tokens: List<String>, title: String, body: String, data: Map<String, String>) {
        if (tokens.isNotEmpty()) log.info("PUSH (simulado) a {} dispositivo(s): {} — {} {}", tokens.size, title, body, data)
    }
}

@Service
class NotificationService(
    private val notifications: NotificationRepository,
    private val devices: DeviceTokenRepository,
    private val users: UserRepository,
    private val push: PushSender,
) {
    fun notify(userId: Long, type: NotificationType, title: String, message: String, route: String?, targetId: Long?) {
        notifications.save(Notification(userId, type, title, message, route, targetId))
        val data = buildMap { route?.let { put("route", it) }; targetId?.let { put("targetId", it.toString()) } }
        push.send(devices.findAllByUserId(userId).map { it.token }, title, message, data)
    }

    fun notifyAdmins(type: NotificationType, title: String, message: String, route: String?, targetId: Long?) =
        users.findAllByRole(Role.ADMINISTRADOR).forEach { notify(it.id!!, type, title, message, route, targetId) }

    fun lowStockCheck(p: Product, before: Int) {
        if (before > p.minStock && p.stock <= p.minStock) {
            notifyAdmins(NotificationType.STOCK_BAJO, "Stock bajo", "${p.name}: quedan ${p.stock} unidades", "admin/inventory", p.id)
        }
    }

    @Transactional(readOnly = true)
    fun list(userId: Long) = notifications.findAllByUserIdOrderByCreatedAtDesc(userId)

    @Transactional
    fun markRead(userId: Long, id: Long) {
        val n = notifications.findByIdOrNull(id)?.takeIf { it.userId == userId } ?: throw NotFoundException("Notificación")
        n.read = true
    }

    @Transactional
    fun registerToken(userId: Long, token: String) {
        val existing = devices.findByToken(token)
        if (existing != null) { existing.userId = userId; existing.updatedAt = Instant.now() }
        else devices.save(DeviceToken(userId, token))
    }
}

// ---------- Pedidos ----------
data class CartValidation(val ok: Boolean, val subtotal: Long, val issues: List<StockIssue>)

@Service
class OrderService(
    private val orders: OrderRepository,
    private val products: ProductRepository,
    private val users: UserRepository,
    private val notifications: NotificationService,
) {
    @Transactional(readOnly = true)
    fun validate(lines: List<Line>): CartValidation {
        val merged = merge(lines)
        val byId = products.findAllById(merged.keys).associateBy { it.id!! }
        val issues = merged.mapNotNull { (id, qty) ->
            val p = byId[id]
            val available = if (p == null || !p.active) 0 else p.stock
            if (qty > available) StockIssue(id, p?.name ?: "#$id", qty, available) else null
        }
        val subtotal = merged.entries.sumOf { (id, q) -> (byId[id]?.price ?: 0L) * q }
        return CartValidation(issues.isEmpty(), subtotal, issues)
    }

    /**
     * Crea el pedido en UNA transacción: bloquea las filas de producto (PESSIMISTIC_WRITE), verifica stock
     * de todos los ítems y lo descuenta. Si algún ítem falla → 409 y no se modifica nada.
     */
    @Transactional
    fun place(userId: Long, lines: List<Line>, mode: DeliveryMode): CustomerOrder {
        val merged = merge(lines)
        if (merged.isEmpty()) throw BadRequestException("EMPTY_CART", "El carrito está vacío")
        val user = users.findByIdOrNull(userId) ?: throw NotFoundException("Usuario")
        if (mode == DeliveryMode.DESPACHO && user.address.isNullOrBlank())
            throw BadRequestException("ADDRESS_REQUIRED", "Se requiere una dirección para el despacho")
        val locked = products.lockAll(merged.keys).associateBy { it.id!! }
        val issues = merged.mapNotNull { (id, qty) ->
            val p = locked[id]
            val available = if (p == null || !p.active) 0 else p.stock
            if (qty > available) StockIssue(id, p?.name ?: "#$id", qty, available) else null
        }
        if (issues.isNotEmpty()) throw StockConflictException(issues)
        val order = CustomerOrder(user = user, deliveryMode = mode)
        merged.forEach { (id, qty) ->
            val p = locked.getValue(id)
            val before = p.stock
            p.stock -= qty
            p.updatedAt = Instant.now()
            order.items += OrderItem(order, id, p.name, p.price, qty)
            notifications.lowStockCheck(p, before)
        }
        order.subtotal = order.items.sumOf { it.unitPrice * it.quantity }
        order.shipping = OrderRules.shipping(order.subtotal, mode)
        order.total = order.subtotal + order.shipping
        orders.saveAndFlush(order)
        order.number = OrderRules.number(order.id!!)
        notifications.notifyAdmins(NotificationType.NUEVO_PEDIDO, "Nuevo pedido ${order.number}",
            "${user.name} realizó un pedido por $${order.total}", "orders", order.id)
        notifications.notify(userId, NotificationType.ESTADO_PEDIDO, "Pedido ${order.number} recibido",
            "Tu pedido quedó pendiente de validación", "orders", order.id)
        return order
    }

    @Transactional(readOnly = true)
    fun list(current: CurrentUser, status: OrderStatus?): List<CustomerOrder> =
        (if (current.isAdmin) orders.findAllByOrderByCreatedAtDesc() else orders.findAllByUserIdOrderByCreatedAtDesc(current.id))
            .filter { status == null || it.status == status }

    @Transactional(readOnly = true)
    fun get(current: CurrentUser, id: Long): CustomerOrder {
        val o = orders.findByIdOrNull(id) ?: throw NotFoundException("Pedido")
        if (!current.isAdmin && o.user.id != current.id) throw NotFoundException("Pedido")
        return o
    }

    @Transactional
    fun changeStatus(id: Long, to: OrderStatus): CustomerOrder {
        val o = orders.findByIdOrNull(id) ?: throw NotFoundException("Pedido")
        if (!OrderRules.canTransition(o.status, to))
            throw ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION", "No se puede pasar de ${o.status} a $to")
        if (to == OrderStatus.CANCELADO) {
            val locked = products.lockAll(o.items.map { it.productId }).associateBy { it.id!! }
            o.items.forEach { locked[it.productId]?.let { p -> p.stock += it.quantity } }
        }
        o.status = to
        notifications.notify(o.user.id!!, NotificationType.ESTADO_PEDIDO, "Pedido ${o.number}",
            "Nuevo estado: ${to.name.replace('_', ' ').lowercase()}", "orders", o.id)
        return o
    }

    private fun merge(lines: List<Line>): Map<Long, Int> =
        lines.filter { it.quantity > 0 }.groupBy { it.productId }.mapValues { (_, l) -> l.sumOf { it.quantity } }.toSortedMap()
}

// ---------- Inventario ----------
data class MovementResult(val movement: StockMovement, val duplicated: Boolean)

@Service
class InventoryService(
    private val products: ProductRepository,
    private val movements: StockMovementRepository,
    private val notifications: NotificationService,
) {
    /** Aplica el movimiento una sola vez por `localId` (reintentos desde la app no duplican). */
    @Transactional
    fun apply(localId: String, productId: Long, type: MovementType, quantity: Int): MovementResult {
        movements.findByLocalId(localId)?.let { return MovementResult(it, true) }
        if (quantity <= 0) throw BadRequestException("INVALID_QUANTITY", "La cantidad debe ser mayor que cero")
        val p = products.lockAll(listOf(productId)).firstOrNull() ?: throw NotFoundException("Producto")
        val before = p.stock
        if (type == MovementType.SALIDA && quantity > p.stock) throw StockConflictException(listOf(StockIssue(productId, p.name, quantity, p.stock)))
        p.stock += if (type == MovementType.ENTRADA) quantity else -quantity
        p.updatedAt = Instant.now()
        notifications.lowStockCheck(p, before)
        return MovementResult(movements.save(StockMovement(localId, productId, type, quantity, p.stock)), false)
    }
}

// ---------- Productos ----------
data class ProductInput(
    val name: String, val categoryId: Long, val description: String, val price: Long, val presentation: String,
    val weightGrams: Int, val origin: String, val stock: Int, val minStock: Int, val active: Boolean, val imageUrl: String?,
)

@Service
class ProductService(private val products: ProductRepository, private val categories: CategoryRepository) {
    @Transactional(readOnly = true)
    fun search(categoryId: Long?, q: String?, active: Boolean?, lowStock: Boolean = false): List<Product> =
        products.findAll().asSequence()
            .filter { categoryId == null || it.category.id == categoryId }
            .filter { q.isNullOrBlank() || it.name.contains(q.trim(), ignoreCase = true) || it.description.contains(q.trim(), ignoreCase = true) }
            .filter { active == null || it.active == active }
            .filter { !lowStock || it.stock <= it.minStock }
            .sortedBy { it.name }
            .toList()

    @Transactional(readOnly = true)
    fun get(id: Long) = products.findByIdOrNull(id) ?: throw NotFoundException("Producto")

    @Transactional(readOnly = true)
    fun byQr(code: String) = products.findByQrCodeIgnoreCase(code.trim()) ?: throw NotFoundException("Producto")

    @Transactional
    fun create(i: ProductInput): Product {
        val p = products.saveAndFlush(Product(i.name, category(i.categoryId), i.description, i.price, i.presentation, i.weightGrams,
            i.origin, i.imageUrl, i.stock, i.minStock, i.active))
        p.qrCode = "OYL-P-%04d".format(p.id)
        return p
    }

    @Transactional
    fun update(id: Long, i: ProductInput): Product = get(id).apply {
        name = i.name; category = category(i.categoryId); description = i.description; price = i.price
        presentation = i.presentation; weightGrams = i.weightGrams; origin = i.origin; stock = i.stock
        minStock = i.minStock; active = i.active; i.imageUrl?.let { imageUrl = it }; updatedAt = Instant.now()
    }

    @Transactional
    fun setActive(id: Long, active: Boolean) = get(id).apply { this.active = active; updatedAt = Instant.now() }

    @Transactional
    fun setImage(id: Long, url: String) = get(id).apply { imageUrl = url; updatedAt = Instant.now() }

    private fun category(id: Long) = categories.findByIdOrNull(id) ?: throw BadRequestException("INVALID_CATEGORY", "Categoría inválida")
}
