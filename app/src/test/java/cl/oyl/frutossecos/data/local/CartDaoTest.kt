package cl.oyl.frutossecos.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import cl.oyl.frutossecos.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CartDaoTest : DbTestBase() {

    @Test
    fun carritosDeDistintosClientesNoSeMezclan() = runTest {
        seed()
        val ana = newClient("ana")
        val luis = newClient("luis")
        val cart = db.cartDao()

        cart.upsert(CartItemEntity(ana, 1, 2))
        cart.upsert(CartItemEntity(ana, 2, 1))
        cart.upsert(CartItemEntity(luis, 1, 5))

        val anaLines = cart.observeLines(ana).first()
        val luisLines = cart.observeLines(luis).first()
        assertEquals(listOf(1L, 2L).toSet(), anaLines.map { it.productId }.toSet())
        assertEquals(2, anaLines.first { it.productId == 1L }.quantity)
        assertEquals(1, luisLines.size)
        assertEquals(5, luisLines.single().quantity)
    }

    @Test
    fun vaciarCarritoDeUnClienteNoAfectaAOtro() = runTest {
        seed()
        val ana = newClient("ana")
        val luis = newClient("luis")
        db.cartDao().upsert(CartItemEntity(ana, 1, 2))
        db.cartDao().upsert(CartItemEntity(luis, 3, 1))

        db.cartDao().clear(ana)

        assertTrue(db.cartDao().observeItems(ana).first().isEmpty())
        assertEquals(1, db.cartDao().observeItems(luis).first().size)
    }

    @Test
    fun actualizarYQuitarSoloTocaAlUsuarioIndicado() = runTest {
        seed()
        val ana = newClient("ana")
        val luis = newClient("luis")
        db.cartDao().upsert(CartItemEntity(ana, 1, 1))
        db.cartDao().upsert(CartItemEntity(luis, 1, 1))

        db.cartDao().updateQuantity(ana, 1, 4)
        db.cartDao().remove(luis, 1)

        assertEquals(4, db.cartDao().getItem(ana, 1)?.quantity)
        assertNull(db.cartDao().getItem(luis, 1))
    }

    @Test
    fun contadorDelCarritoEsPorUsuario() = runTest {
        seed()
        val ana = newClient("ana")
        val luis = newClient("luis")
        db.cartDao().observeCount(ana).test {
            assertEquals(0, awaitItem())
            db.cartDao().upsert(CartItemEntity(ana, 1, 3))
            assertEquals(3, awaitItem())
            db.cartDao().upsert(CartItemEntity(luis, 2, 7))
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun borrarUsuarioBorraSoloSuCarrito() = runTest {
        seed()
        val ana = newClient("ana")
        val luis = newClient("luis")
        db.cartDao().upsert(CartItemEntity(ana, 1, 1))
        db.cartDao().upsert(CartItemEntity(luis, 1, 1))
        db.openHelper.writableDatabase.execSQL("DELETE FROM users WHERE id = $ana")
        assertTrue(db.cartDao().observeItems(ana).first().isEmpty())
        assertEquals(1, db.cartDao().observeItems(luis).first().size)
    }
}
