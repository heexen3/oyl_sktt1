package cl.oyl.frutossecos.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import cl.oyl.frutossecos.MainActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renderiza Bienvenida, Inicio (banner verde) y Catálogo en modo claro y oscuro a PNG para revisar
 * contraste visualmente. Carpeta: propiedad de sistema `oyl.screenshotDir` (por defecto app/build/screenshots).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ContrastScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val outDir = File(System.getProperty("oyl.screenshotDir") ?: "build/screenshots").apply { mkdirs() }

    private fun waitText(text: String, timeout: Long = 15_000) =
        rule.waitUntil(timeout) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun click(text: String) {
        waitText(text)
        rule.onAllNodesWithText(text).onFirst().performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun type(label: String, value: String) {
        val m = hasSetTextAction() and (hasText(label) or hasAnyAncestor(hasText(label)))
        rule.waitUntil(10_000) { rule.onAllNodes(m).fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodes(m).onFirst().performScrollTo().performTextInput(value)
    }

    private fun shot(name: String) {
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val f = File(outDir, "$name.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(f.length() > 0)
    }

    private fun recorrido(mode: String) {
        waitText("OYL Frutos Secos")
        shot("welcome-$mode")
        click("Comenzar")
        click("¿No tienes cuenta? Regístrate")
        type("Nombre", "Luis Pérez")
        type("Alias", "luis")
        type("Correo electrónico", "luis@correo.cl")
        type("Contraseña", "clave12345")
        type("Teléfono", "+56 9 5555 4444")
        click("Crear mi cuenta")
        waitText("Accesos rápidos", 30_000)
        shot("home-$mode")
        click("Catálogo")
        waitText("Almendras")
        shot("catalog-$mode")
    }

    @Test @Config(qualifiers = "+notnight") fun capturas_modo_claro() = recorrido("light")

    @Test @Config(qualifiers = "+night") fun capturas_modo_oscuro() = recorrido("dark")
}
