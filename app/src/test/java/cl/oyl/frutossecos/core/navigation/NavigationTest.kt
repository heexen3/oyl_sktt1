package cl.oyl.frutossecos.core.navigation

import cl.oyl.frutossecos.domain.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    @Test fun existenLas15Rutas() {
        assertEquals(15, Screen.entries.size)
        assertEquals(15, Screen.entries.map { it.route }.toSet().size)
    }

    @Test fun barraInferiorCliente() {
        assertEquals(
            listOf(Screen.Home, Screen.Map, Screen.Scanner, Screen.Orders, Screen.Profile),
            bottomItemsFor(Role.CLIENTE).map { it.screen },
        )
    }

    @Test fun barraInferiorAdmin() {
        assertEquals(
            listOf(Screen.Home, Screen.AdminInventory, Screen.Orders, Screen.Profile),
            bottomItemsFor(Role.ADMINISTRADOR).map { it.screen },
        )
        assertTrue(bottomItemsFor(null).isEmpty())
    }

    @Test fun permisosPorRol() {
        assertFalse(Screen.AdminProducts.isAllowedFor(Role.CLIENTE))
        assertTrue(Screen.AdminProducts.isAllowedFor(Role.ADMINISTRADOR))
        assertFalse(Screen.Cart.isAllowedFor(Role.ADMINISTRADOR))
        assertFalse(Screen.Home.isAllowedFor(null))
        assertTrue(Screen.Welcome.isAllowedFor(null))
        assertEquals(Screen.ProductDetail, Screen.fromRoute("product/{id}"))
    }
}
