package cl.oyl.backend.repo

import cl.oyl.backend.domain.Category
import cl.oyl.backend.domain.CustomerOrder
import cl.oyl.backend.domain.DeviceToken
import cl.oyl.backend.domain.Notification
import cl.oyl.backend.domain.OrderStatus
import cl.oyl.backend.domain.Product
import cl.oyl.backend.domain.Role
import cl.oyl.backend.domain.StockMovement
import cl.oyl.backend.domain.Store
import cl.oyl.backend.domain.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<UserAccount, Long> {
    fun findByAliasIgnoreCase(alias: String): UserAccount?
    fun findByEmailIgnoreCase(email: String): UserAccount?
    fun existsByAliasIgnoreCase(alias: String): Boolean
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun findAllByRole(role: Role): List<UserAccount>
}

interface CategoryRepository : JpaRepository<Category, Long>

interface ProductRepository : JpaRepository<Product, Long> {
    fun findByQrCodeIgnoreCase(code: String): Product?

    /** Bloquea las filas (SELECT ... FOR UPDATE) en orden de id para evitar interbloqueos y sobreventas. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    fun lockAll(@Param("ids") ids: Collection<Long>): List<Product>
}

interface OrderRepository : JpaRepository<CustomerOrder, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<CustomerOrder>
    fun findAllByOrderByCreatedAtDesc(): List<CustomerOrder>
    fun countByStatus(status: OrderStatus): Long
}

interface StockMovementRepository : JpaRepository<StockMovement, Long> {
    fun findByLocalId(localId: String): StockMovement?
}

interface StoreRepository : JpaRepository<Store, Long>

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
}

interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByToken(token: String): DeviceToken?
    fun findAllByUserId(userId: Long): List<DeviceToken>
}
