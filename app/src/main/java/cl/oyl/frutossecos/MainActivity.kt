package cl.oyl.frutossecos

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OylTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                if (state.loading) LoadingState()
                else OylApp(state = state, onPreview = viewModel::preview, onLogout = viewModel::logout)
            }
        }
    }
}
