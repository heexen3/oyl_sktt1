package cl.oyl.backend.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

enum class Role { CLIENTE, ADMINISTRADOR }
enum class OrderStatus { PENDIENTE_VALIDACION, CONFIRMADO, PREPARADO, ENTREGADO, CANCELADO }
enum class DeliveryMode { RETIRO, DESPACHO }
enum class MovementType { ENTRADA, SALIDA }
enum class StoreType { TIENDA, RETIRO }
enum class NotificationType { NUEVO_PEDIDO, ESTADO_PEDIDO, STOCK_BAJO, GENERAL }

@Entity
@Table(name = "users")
class UserAccount(
    var name: String,
    @Column(unique = true) var alias: String,
    @Column(unique = true) var email: String,
    var phone: String,
    var address: String? = null,
    var avatarId: Int? = null,
    var passwordHash: String,
    @Enumerated(EnumType.STRING) var role: Role = Role.CLIENTE,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

@Entity
class Category(var name: String, @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)

@Entity
class Product(
    var name: String,
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "category_id") var category: Category,
    @Column(length = 1000) var description: String,
    var price: Long,
    var presentation: String,
    var weightGrams: Int,
    var origin: String,
    var imageUrl: String? = null,
    var stock: Int,
    var minStock: Int,
    var active: Boolean = true,
    @Column(unique = true) var qrCode: String? = null,
    var updatedAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

@Entity
@Table(name = "orders", indexes = [Index(columnList = "user_id")])
class CustomerOrder(
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id") var user: UserAccount,
    @Column(name = "order_number") var number: String = "",
    @Enumerated(EnumType.STRING) var status: OrderStatus = OrderStatus.PENDIENTE_VALIDACION,
    var subtotal: Long = 0,
    var shipping: Long = 0,
    var total: Long = 0,
    @Enumerated(EnumType.STRING) var deliveryMode: DeliveryMode,
    var createdAt: Instant = Instant.now(),
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    var items: MutableList<OrderItem> = mutableListOf(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

@Entity
class OrderItem(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") var order: CustomerOrder,
    var productId: Long,
    var name: String,
    var unitPrice: Long,
    var quantity: Int,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

/** Movimiento de inventario. `localId` (UUID generado en la app) garantiza idempotencia. */
@Entity
class StockMovement(
    @Column(unique = true, nullable = false) var localId: String,
    var productId: Long,
    @Enumerated(EnumType.STRING) var type: MovementType,
    var quantity: Int,
    var resultingStock: Int,
    var createdAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

@Entity
class Store(
    var name: String,
    var address: String,
    var lat: Double,
    var lng: Double,
    var hours: String,
    @Enumerated(EnumType.STRING) var type: StoreType,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

@Entity
@Table(indexes = [Index(columnList = "userId")])
class Notification(
    var userId: Long,
    @Enumerated(EnumType.STRING) var type: NotificationType,
    var title: String,
    var message: String,
    var targetRoute: String? = null,
    var targetId: Long? = null,
    @Column(name = "is_read") var read: Boolean = false,
    var createdAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)

@Entity
class DeviceToken(
    var userId: Long,
    @Column(unique = true, length = 512) var token: String,
    var updatedAt: Instant = Instant.now(),
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
)
