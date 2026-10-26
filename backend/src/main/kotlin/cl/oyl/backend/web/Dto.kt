package cl.oyl.backend.web

import cl.oyl.backend.domain.CustomerOrder
import cl.oyl.backend.domain.DeliveryMode
import cl.oyl.backend.domain.MovementType
import cl.oyl.backend.domain.Notification
import cl.oyl.backend.domain.OrderStatus
import cl.oyl.backend.domain.Product
import cl.oyl.backend.domain.Role
import cl.oyl.backend.domain.Store
import cl.oyl.backend.domain.UserAccount
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 2, max = 60) val name: String,
    @field:Pattern(regexp = "^[A-Za-z0-9._]{3,20}$") val alias: String,
    @field:Email @field:NotBlank val email: String,
    @field:Size(min = 8, max = 64) val password: String,
    @field:Pattern(regexp = "^\\+?56\\s?9\\s?\\d{4}\\s?\\d{4}$") val phone: String,
    val address: String? = null,
    val avatarId: Int? = null,
)
data class LoginRequest(@field:NotBlank val login: String, @field:NotBlank val password: String, val role: Role)
data class UserDto(val id: Long, val name: String, val alias: String, val email: String, val phone: String, val address: String?, val avatarId: Int?, val role: Role)
data class AuthResponse(val token: String, val user: UserDto)

data class ProductDto(
    val id: Long, val name: String, val categoryId: Long, val category: String, val description: String, val price: Long,
    val presentation: String, val weightGrams: Int, val origin: String, val imageUrl: String?, val stock: Int,
    val minStock: Int, val active: Boolean, val qrCode: String?, val updatedAt: Instant,
)
data class ProductRequest(
    @field:NotBlank @field:Size(max = 80) val name: String,
    val categoryId: Long,
    @field:NotBlank val description: String,
    @field:Min(1) val price: Long,
    @field:NotBlank val presentation: String,
    @field:Min(1) val weightGrams: Int,
    @field:NotBlank val origin: String,
    @field:Min(0) val stock: Int,
    @field:Min(0) val minStock: Int,
    val active: Boolean = true,
    val imageUrl: String? = null,
)
data class ActiveRequest(val active: Boolean)

data class LineRequest(val productId: Long, @field:Min(1) val quantity: Int)
data class CartValidateRequest(@field:NotEmpty @field:Valid val items: List<LineRequest>)
data class CreateOrderRequest(@field:NotEmpty @field:Valid val items: List<LineRequest>, val deliveryMode: DeliveryMode)
data class StatusRequest(val status: OrderStatus)
data class OrderItemDto(val productId: Long, val name: String, val unitPrice: Long, val quantity: Int)
data class OrderDto(
    val id: Long, val number: String, val userId: Long, val customerName: String, val status: OrderStatus,
    val subtotal: Long, val shipping: Long, val total: Long, val deliveryMode: DeliveryMode, val createdAt: Instant, val items: List<OrderItemDto>,
)

data class MovementRequest(@field:NotBlank val localId: String, val productId: Long, val type: MovementType, @field:Min(1) val quantity: Int)
data class MovementResponse(val localId: String, val productId: Long, val type: MovementType, val quantity: Int, val resultingStock: Int, val duplicated: Boolean)

data class StoreDto(val id: Long, val name: String, val address: String, val lat: Double, val lng: Double, val hours: String, val type: String)
data class NotificationDto(val id: Long, val type: String, val title: String, val message: String, val createdAt: Instant, val read: Boolean, val targetRoute: String?, val targetId: Long?)
data class FcmTokenRequest(@field:NotBlank val token: String)

data class ErrorResponse(val code: String, val message: String, val details: Any? = null)

fun UserAccount.toDto() = UserDto(id!!, name, alias, email, phone, address, avatarId, role)
fun Product.toDto() = ProductDto(id!!, name, category.id!!, category.name, description, price, presentation, weightGrams, origin, imageUrl, stock, minStock, active, qrCode, updatedAt)
fun CustomerOrder.toDto() = OrderDto(id!!, number, user.id!!, user.name, status, subtotal, shipping, total, deliveryMode, createdAt,
    items.map { OrderItemDto(it.productId, it.name, it.unitPrice, it.quantity) })
fun Store.toDto() = StoreDto(id!!, name, address, lat, lng, hours, type.name)
fun Notification.toDto() = NotificationDto(id!!, type.name, title, message, createdAt, read, targetRoute, targetId)
