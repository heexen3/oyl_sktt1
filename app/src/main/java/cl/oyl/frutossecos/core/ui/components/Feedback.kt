package cl.oyl.frutossecos.core.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource

/** Snackbar compartido por todas las pantallas (lo crea el Scaffold de OylApp). */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** Mensaje de una sola vez: res de string + argumento opcional. */
data class UiMessage(val res: Int, val arg: String? = null, val id: Long = System.nanoTime())

@Composable
fun MessageEffect(message: UiMessage?, onShown: () -> Unit) {
    val snackbar = LocalSnackbar.current
    val text = message?.let { if (it.arg != null) stringResource(it.res, it.arg) else stringResource(it.res) }
    LaunchedEffect(message?.id) {
        if (text != null) {
            // Se limpia después de mostrarse: limpiar antes cancelaría este efecto.
            snackbar.showSnackbar(text)
            onShown()
        }
    }
}
