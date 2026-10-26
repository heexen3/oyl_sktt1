package cl.oyl.frutossecos.domain.model

/** Modelos de dominio (independientes de Room y de la red). Montos en CLP enteros. */
data class User(
    val id: Long,
    val name: String,
    val alias: String,
    val email: String,
    val phone: String,
    val address: String?,
    val avatarId: Int?,
    val avatarUri: String?,
    val role: Role,
    val notificationsEnabled: Boolean = true,
)

data class Session(val userId: Long, val role: Role, val token: String?, val loggedAt: Long)

data class Category(val id: Long, val name: String)

data class Product(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val description: String,
    val price: Long,
    val presentation: String,
    val weightGrams: Int,
    val origin: String,
    val imageUrl: String?,
    val stock: Int,
    val minStock: Int,
    val active: Boolean,
    val qrCode: String,
) {
    val isLowStock: Boolean get() = stock in 1..minStock
    val isOutOfStock: Boolean get() = stock <= 0
}

data class CartLine(
    val productId: Long,
    val name: String,
    val unitPrice: Long,
    val quantity: Int,
    val stock: Int,
    val imageUrl: String?,
) {
    val subtotal: Long get() = unitPrice * quantity
}

data class Store(
    val id: Long,
    val name: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val hours: String,
    val type: StoreType,
)

data class Order(
    val id: Long,
    val userId: Long,
    val number: String,
    val status: OrderStatus,
    val total: Long,
    val shipping: Long,
    val deliveryMode: DeliveryMode,
    val createdAt: Long,
)

data class OrderItem(val productId: Long, val name: String, val unitPrice: Long, val quantity: Int) {
    val subtotal: Long get() = unitPrice * quantity
}

data class AppNotification(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAt: Long,
    val read: Boolean,
    val targetRoute: String?,
    val targetId: Long?,
)
