package cl.oyl.frutossecos.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasAnyAncestor
import cl.oyl.frutossecos.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Prueba de humo de extremo a extremo (Robolectric + Compose): arranca la app real con Hilt y Room,
 * inicia sesión y recorre las pantallas para verificar que ninguna falla al abrirse.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitText(text: String, timeout: Long = 15_000) =
        rule.waitUntil(timeout) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun click(text: String) {
        waitText(text)
        rule.onAllNodesWithText(text).onFirst().performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun clickNoScroll(text: String) {
        waitText(text)
        rule.onAllNodesWithText(text).onFirst().performClick()
        rule.waitForIdle()
    }

    private fun type(label: String, value: String) {
        val m = hasSetTextAction() and (hasText(label) or hasAnyAncestor(hasText(label)))
        rule.waitUntil(10_000) { rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(m).onFirst().performScrollTo().performTextInput(value)
    }

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private fun visitFromHome(tile: String, expect: String) {
        click(tile)
        waitText(expect)
        back()
        waitText("Accesos rápidos")
    }

    @Test fun administrador_recorre_sus_pantallas() {
        click("Ya tengo una cuenta")
        clickNoScroll("Administrador")
        type("Alias o correo", "admin")
        type("Contraseña", "OylAdmin2026!")
        click("Ingresar")
        waitText("Accesos rápidos", 30_000)
        visitFromHome("Gestión de productos", "Gestión de productos")
        visitFromHome("Gestión de inventario", "Gestión de inventario")
        visitFromHome("Catálogo", "Catálogo")
        visitFromHome("Escáner QR", "Escáner QR")
        visitFromHome("Notificaciones", "Notificaciones")
        clickNoScroll("Pedidos"); rule.waitForIdle()
        clickNoScroll("Perfil"); waitText("Perfil y configuración")
        clickNoScroll("Inventario"); waitText("Gestión de inventario")
        clickNoScroll("Inicio"); waitText("Accesos rápidos")
    }

    @Test fun cliente_se_registra_compra_y_recorre_sus_pantallas() {
        click("Comenzar")
        click("¿No tienes cuenta? Regístrate")
        type("Nombre", "Carla Soto")
        type("Alias", "carla")
        type("Correo electrónico", "carla@correo.cl")
        type("Contraseña", "clave12345")
        type("Teléfono", "+56 9 5555 4444")
        click("Crear mi cuenta")
        waitText("Accesos rápidos", 30_000)
        // Compra completa: catálogo → carrito → confirmación → mis pedidos
        click("Catálogo")
        val add = hasContentDescription("al carrito", substring = true)
        rule.waitUntil(15_000) { rule.onAllNodes(add).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(add).onFirst().performClick()
        rule.waitForIdle()
        back(); waitText("Accesos rápidos")
        click("Mi carrito")
        clickNoScroll("Continuar compra")
        clickNoScroll("Confirmar pedido")
        waitText("¡Pedido recibido!")
        clickNoScroll("Ver mis pedidos")
        rule.waitUntil(15_000) { rule.onAllNodes(hasText("OYL-", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        clickNoScroll("Inicio"); waitText("Accesos rápidos")
        visitFromHome("Notificaciones", "Notificaciones")
        clickNoScroll("Mapa"); waitText("Mapa de tiendas")
        clickNoScroll("Escáner"); waitText("Escáner QR")
        clickNoScroll("Pedidos"); rule.waitForIdle()
        clickNoScroll("Perfil"); waitText("Perfil y configuración")
        clickNoScroll("Inicio"); waitText("Accesos rápidos")
    }
}
