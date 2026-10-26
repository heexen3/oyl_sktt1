package cl.oyl.frutossecos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val loading: Boolean = true,
    val role: Role? = null,
    val userId: Long? = null,
)

/** Estado global: sesión activa persistida en Room (sobrevive reinicios de la app). */
@HiltViewModel
class MainViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    val uiState: StateFlow<MainUiState> = authRepository.session.map { session ->
        MainUiState(loading = false, role = session?.role, userId = session?.userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
