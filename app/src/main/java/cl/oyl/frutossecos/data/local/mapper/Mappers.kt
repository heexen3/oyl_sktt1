package cl.oyl.frutossecos.data.local.mapper

import cl.oyl.frutossecos.data.local.dao.CartLineRow
import cl.oyl.frutossecos.data.local.entity.CategoryEntity
import cl.oyl.frutossecos.data.local.entity.NotificationEntity
import cl.oyl.frutossecos.data.local.entity.OrderEntity
import cl.oyl.frutossecos.data.local.entity.OrderItemEntity
import cl.oyl.frutossecos.data.local.entity.ProductEntity
import cl.oyl.frutossecos.data.local.entity.StoreEntity
import cl.oyl.frutossecos.data.local.entity.UserEntity
import cl.oyl.frutossecos.domain.model.AppNotification
import cl.oyl.frutossecos.domain.model.CartLine
import cl.oyl.frutossecos.domain.model.Category
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.OrderItem
import cl.oyl.frutossecos.domain.model.Product
import cl.oyl.frutossecos.domain.model.Store
import cl.oyl.frutossecos.domain.model.User

fun UserEntity.toDomain() = User(id, name, alias, email, phone, address, avatarId, avatarUri, role, notificationsEnabled)
fun CategoryEntity.toDomain() = Category(id, name)
fun ProductEntity.toDomain() = Product(
    id, name, categoryId, description, price, presentation, weightGrams, origin,
    imagePath ?: imageUrl, stock, minStock, active, qrCode,
)
fun CartLineRow.toDomain() = CartLine(productId, name, price, quantity, stock, imageUrl)
fun StoreEntity.toDomain() = Store(id, name, address, lat, lng, hours, type)
fun OrderEntity.toDomain() = Order(id, userId, number, status, total, shipping, deliveryMode, createdAt)
fun OrderItemEntity.toDomain() = OrderItem(productId, name, unitPrice, quantity)
fun NotificationEntity.toDomain() = AppNotification(id, type, title, message, createdAt, read, targetRoute, targetId)
