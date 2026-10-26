package cl.oyl.frutossecos.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.oyl.frutossecos.core.util.PasswordHasher
import cl.oyl.frutossecos.data.local.entity.NotificationEntity
import cl.oyl.frutossecos.data.local.entity.OrderEntity
import cl.oyl.frutossecos.data.local.entity.PendingStockMovementEntity
import cl.oyl.frutossecos.data.local.entity.SessionEntity
import cl.oyl.frutossecos.data.local.seed.SeedData
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.NotificationType
import cl.oyl.frutossecos.domain.model.OrderStatus
import cl.oyl.frutossecos.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SeedAndDaoTest : DbTestBase() {

    @Test
    fun seedCreaAdminDemoConHashYSal() = runTest {
        seed()
        val admin = db.userDao().findByEmail("admin@admin.cl")
        assertNotNull(admin)
        admin!!
        assertEquals("Administrador", admin.name)
        assertEquals("+56 9 1234 1234", admin.phone)
        assertEquals("Santiago", admin.address)
        assertEquals(Role.ADMINISTRADOR, admin.role)
        assertNotEquals(SeedData.DEMO_ADMIN_PASSWORD, admin.passwordHash)
        assertTrue(PasswordHasher.verify(SeedData.DEMO_ADMIN_PASSWORD, admin.passwordHash, admin.salt))
        assertFalse(PasswordHasher.verify("otra", admin.passwordHash, admin.salt))
    }

    @Test
    fun seedEsIdempotente() = runTest {
        seed(); seed()
        assertEquals(1, db.userDao().countByRole(Role.ADMINISTRADOR))
        assertEquals(SeedData.categories.size, db.categoryDao().count())
        assertEquals(SeedData.stores.size, db.storeDao().count())
        assertEquals(SeedData.products(0).size, db.productDao().count())
    }

    @Test
    fun productosTienenQrYSeResuelvenPorQr() = runTest {
        seed()
        val p = db.productDao().findByQr("OYL-P-0001")
        assertEquals("Almendras", p?.name)
        assertNull(db.productDao().findByQr("QR-INVALIDO"))
    }

    @Test
    fun catalogoFiltraPorCategoriaYTexto() = runTest {
        seed()
        val semillas = db.productDao().observe(categoryId = SeedData.CATEGORY_SEMILLAS).first()
        assertTrue(semillas.isNotEmpty() && semillas.all { it.categoryId == SeedData.CATEGORY_SEMILLAS })
        val nueces = db.productDao().observe(query = "nuec").first()
        assertEquals(listOf("Nueces"), nueces.map { it.name })
    }

    @Test
    fun descuentoCondicionalNoDejaStockNegativo() = runTest {
        seed()
        val p = db.productDao().getById(5)!! // stock 4
        assertEquals(0, db.productDao().decrementIfAvailable(5, p.stock + 1, 2L))
        assertEquals(1, db.productDao().decrementIfAvailable(5, p.stock, 2L))
        assertEquals(0, db.productDao().getById(5)!!.stock)
    }

    @Test
    fun sesionUnicaYLogoutSoloBorraSesion() = runTest {
        seed()
        val ana = newClient("ana")
        db.sessionDao().save(SessionEntity(userId = ana, role = Role.CLIENTE, token = "t1", loggedAt = 1))
        val admin = db.userDao().findByEmail(SeedData.ADMIN_EMAIL)!!
        db.sessionDao().save(SessionEntity(userId = admin.id, role = Role.ADMINISTRADOR, token = "t2", loggedAt = 2))
        assertEquals(admin.id, db.sessionDao().get()?.userId)
        db.sessionDao().clear()
        assertNull(db.sessionDao().get())
        assertNotNull(db.userDao().getById(ana))
    }

    @Test
    fun pedidosYNotificacionesFiltranPorUsuario() = runTest {
        seed()
        val ana = newClient("ana")
        val luis = newClient("luis")
        db.orderDao().upsertOrder(OrderEntity(1, ana, "OYL-0001", OrderStatus.PENDIENTE_VALIDACION, 5000, 0, DeliveryMode.RETIRO, 1))
        db.orderDao().upsertOrder(OrderEntity(2, luis, "OYL-0002", OrderStatus.CONFIRMADO, 7000, 0, DeliveryMode.DESPACHO, 2))
        assertEquals(listOf("OYL-0001"), db.orderDao().observeForUser(ana).first().map { it.number })
        assertEquals(2, db.orderDao().observeAll().first().size)
        assertEquals(1, db.orderDao().observeAll(OrderStatus.CONFIRMADO).first().size)

        db.notificationDao().insert(NotificationEntity(userId = ana, type = NotificationType.GENERAL, title = "a", message = "m", createdAt = 1))
        val id = db.notificationDao().insert(NotificationEntity(userId = luis, type = NotificationType.GENERAL, title = "l", message = "m", createdAt = 1))
        db.notificationDao().markRead(ana, id) // id de otro usuario: no debe marcarse
        assertEquals(1, db.notificationDao().observeUnreadCount(luis).first())
        assertEquals(1, db.notificationDao().observeForUser(ana).first().size)
    }

    @Test
    fun movimientoPendienteEsIdempotentePorLocalId() = runTest {
        seed()
        val m = PendingStockMovementEntity("uuid-1", 1, MovementType.ENTRADA, 5, 1)
        db.pendingStockMovementDao().insert(m)
        db.pendingStockMovementDao().insert(m.copy(quantity = 99))
        val pending = db.pendingStockMovementDao().getPending()
        assertEquals(1, pending.size)
        assertEquals(5, pending.single().quantity)
    }
}
