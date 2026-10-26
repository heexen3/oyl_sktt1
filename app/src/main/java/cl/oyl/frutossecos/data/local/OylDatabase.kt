package cl.oyl.frutossecos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import cl.oyl.frutossecos.data.local.dao.CartDao
import cl.oyl.frutossecos.data.local.dao.CategoryDao
import cl.oyl.frutossecos.data.local.dao.NotificationDao
import cl.oyl.frutossecos.data.local.dao.OrderDao
import cl.oyl.frutossecos.data.local.dao.PendingStockMovementDao
import cl.oyl.frutossecos.data.local.dao.ProductDao
import cl.oyl.frutossecos.data.local.dao.SessionDao
import cl.oyl.frutossecos.data.local.dao.StoreDao
import cl.oyl.frutossecos.data.local.dao.UserDao
import cl.oyl.frutossecos.data.local.entity.CartItemEntity
import cl.oyl.frutossecos.data.local.entity.CategoryEntity
import cl.oyl.frutossecos.data.local.entity.NotificationEntity
import cl.oyl.frutossecos.data.local.entity.OrderEntity
import cl.oyl.frutossecos.data.local.entity.OrderItemEntity
import cl.oyl.frutossecos.data.local.entity.PendingStockMovementEntity
import cl.oyl.frutossecos.data.local.entity.ProductEntity
import cl.oyl.frutossecos.data.local.entity.SessionEntity
import cl.oyl.frutossecos.data.local.entity.StoreEntity
import cl.oyl.frutossecos.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class, SessionEntity::class, CategoryEntity::class, ProductEntity::class,
        CartItemEntity::class, OrderEntity::class, OrderItemEntity::class, NotificationEntity::class,
        StoreEntity::class, PendingStockMovementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class OylDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun storeDao(): StoreDao
    abstract fun pendingStockMovementDao(): PendingStockMovementDao

    companion object { const val NAME = "oyl_frutos_secos.db" }
}
