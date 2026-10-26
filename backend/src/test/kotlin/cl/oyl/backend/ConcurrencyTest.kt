package cl.oyl.backend

import cl.oyl.backend.domain.DeliveryMode
import cl.oyl.backend.domain.Role
import cl.oyl.backend.domain.UserAccount
import cl.oyl.backend.repo.ProductRepository
import cl.oyl.backend.repo.UserRepository
import cl.oyl.backend.service.Line
import cl.oyl.backend.service.OrderService
import cl.oyl.backend.service.StockConflictException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext

/** Dos (o más) pedidos simultáneos por la última unidad: solo uno gana, nunca hay sobreventa. */
@SpringBootTest
@DirtiesContext
class ConcurrencyTest(
    @Autowired private val orders: OrderService,
    @Autowired private val products: ProductRepository,
    @Autowired private val users: UserRepository,
) {
    @Test
    fun `solo un pedido obtiene la ultima unidad`() {
        val p = products.findAll().first { it.name == "Almendras" }
        p.stock = 1
        products.saveAndFlush(p)
        val clients = (1..8).map { users.save(UserAccount("C$it", "c$it", "c$it@correo.cl", "+56 9 1111 2222", null, null, "x", Role.CLIENTE)).id!! }
        val start = CountDownLatch(1)
        val ok = AtomicInteger()
        val conflicts = AtomicInteger()
        val pool = Executors.newFixedThreadPool(clients.size)
        val futures = clients.map { uid ->
            pool.submit {
                start.await()
                try { orders.place(uid, listOf(Line(p.id!!, 1)), DeliveryMode.RETIRO); ok.incrementAndGet() }
                catch (_: StockConflictException) { conflicts.incrementAndGet() }
            }
        }
        start.countDown()
        futures.forEach { it.get() }
        pool.shutdown()
        assertEquals(1, ok.get())
        assertEquals(clients.size - 1, conflicts.get())
        assertEquals(0, products.findById(p.id!!).get().stock)
    }
}
