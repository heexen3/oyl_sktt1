package cl.oyl.frutossecos.data.repository

import cl.oyl.frutossecos.core.notifications.SystemNotifier
import cl.oyl.frutossecos.data.local.DbTestBase
import cl.oyl.frutossecos.data.repository.local.LocalCartRepository
import cl.oyl.frutossecos.data.repository.local.LocalInventoryRepository
import cl.oyl.frutossecos.data.repository.local.LocalOrderRepository
import cl.oyl.frutossecos.data.repository.local.NotificationPublisher
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.NotificationType
import cl.oyl.frutossecos.domain.model.OrderStatus
import cl.oyl.frutossecos.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OrderFlowTest : DbTestBase() {
    private val shown = mutableListOf<String>()
    private val notifier = object : SystemNotifier {
        override fun show(id: Int, title: String, message: String, targetRoute: String?) { shown += title }
    }
    private fun publisher() = NotificationPublisher(db, notifier)
    private fun cart() = LocalCartRepository(db)
    private fun orders() = LocalOrderRepository(db, publisher())
    private fun inventory() = LocalInventoryRepository(db, publisher())

    @Test fun carrito_no_supera_el_stock() = runTest {
        seed()
        val user = newClient("ana")
        val p = db.productDao().getById(1)!!
        assertEquals(p.stock, cart().add(user, p.id, p.stock + 10))
    }

    @Test fun pedido_descuenta_stock_vacia_carrito_y_notifica_admin_y_cliente() = runTest {
        seed()
        val user = newClient("ana")
        val admin = db.userDao().idsByRole(Role.ADMINISTRADOR).first()
        val p = db.productDao().getById(1)!!
        cart().add(user, p.id, 2)
        val order = orders().placeOrder(user, DeliveryMode.RETIRO)
        assertEquals(OrderStatus.PENDIENTE_VALIDACION, order.status)
        assertTrue(order.number.matches(Regex("OYL-\\d{6}")))
        assertEquals(p.price * 2, order.total)
        assertEquals(0L, order.shipping)
        assertEquals(p.stock - 2, db.productDao().getById(p.id)!!.stock)
        assertEquals(0, cart().observeCount(user).first())
        assertEquals(2, orders().observeItems(order.id).first().sumOf { it.quantity })
        val repoN = cl.oyl.frutossecos.data.repository.local.LocalNotificationRepository(db)
        assertTrue(repoN.observe(admin).first().any { it.type == NotificationType.NUEVO_PEDIDO && it.targetId == order.id })
        assertTrue(repoN.observe(user).first().isNotEmpty())
        // Cada cliente ve solo sus pedidos; el admin ve todos.
        val other = newClient("beto")
        assertEquals(0, orders().observeOrders(other, Role.CLIENTE).first().size)
        assertEquals(1, orders().observeOrders(admin, Role.ADMINISTRADOR).first().size)
    }

    @Test fun pedido_sin_stock_suficiente_falla_sin_cambios() = runTest {
        seed()
        val user = newClient("ana")
        val p = db.productDao().getById(1)!!
        cart().add(user, p.id, 3)
        db.productDao().updateStock(p.id, 1, 2_000L)
        try { orders().placeOrder(user, DeliveryMode.DESPACHO); fail() } catch (e: StockException) {
            assertEquals(p.id, e.issues.single().productId)
        }
        assertEquals(1, db.productDao().getById(p.id)!!.stock)
        assertEquals(3, cart().observeCount(user).first())
        try { orders().placeOrder(newClient("vacio"), DeliveryMode.RETIRO); fail() } catch (_: EmptyCartException) {}
    }

    @Test fun transiciones_de_estado_y_cancelacion_devuelve_stock() = runTest {
        seed()
        val user = newClient("ana")
        val p = db.productDao().getById(1)!!
        cart().add(user, p.id, 2)
        val o = orders().placeOrder(user, DeliveryMode.RETIRO)
        try { orders().updateStatus(o.id, OrderStatus.ENTREGADO); fail() } catch (_: InvalidTransitionException) {}
        orders().updateStatus(o.id, OrderStatus.CONFIRMADO)
        orders().updateStatus(o.id, OrderStatus.CANCELADO)
        assertEquals(OrderStatus.CANCELADO, orders().observeOrder(o.id).first()!!.status)
        assertEquals(p.stock, db.productDao().getById(p.id)!!.stock)
    }

    @Test fun ajuste_de_inventario_encola_movimiento_y_valida_salida() = runTest {
        seed()
        val p = db.productDao().getById(1)!!
        assertEquals(p.stock + 5, inventory().adjust(p.id, MovementType.ENTRADA, 5))
        assertEquals(p.stock + 2, inventory().adjust(p.id, MovementType.SALIDA, 3))
        assertEquals(2, inventory().observePendingMovements().first())
        try { inventory().adjust(p.id, MovementType.SALIDA, 10_000); fail() } catch (_: StockException) {}
    }
}
