package cl.oyl.frutossecos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val loading: Boolean = true,
    val role: Role? = null,
    val userId: Long? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {

    /** Rol de vista previa (solo Fase 1, en memoria) mientras no existe el login real. */
    private val previewRole = MutableStateFlow<Role?>(null)

    val uiState: StateFlow<MainUiState> = combine(authRepository.session, previewRole) { session, preview ->
        MainUiState(loading = false, role = session?.role ?: preview, userId = session?.userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun preview(role: Role) { previewRole.value = role }

    fun logout() {
        previewRole.value = null
        viewModelScope.launch { authRepository.logout() }
    }
}
