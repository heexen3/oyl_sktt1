package cl.oyl.frutossecos

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import cl.oyl.frutossecos.core.notifications.AndroidSystemNotifier
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.oyl.frutossecos.core.navigation.OylApp
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.theme.OylTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    /** Ruta pedida por una notificación del sistema; la consume OylApp cuando hay sesión. */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute.value = intent?.getStringExtra(AndroidSystemNotifier.EXTRA_ROUTE)
        setContent {
            OylTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                if (state.loading) LoadingState()
                else OylApp(
                    state = state,
                    onLogout = viewModel::logout,
                    pendingRoute = pendingRoute.value,
                    onPendingRouteHandled = { pendingRoute.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(AndroidSystemNotifier.EXTRA_ROUTE)?.let { pendingRoute.value = it }
    }
}
