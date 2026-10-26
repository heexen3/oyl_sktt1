package cl.oyl.frutossecos.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.data.repository.AuthError
import cl.oyl.frutossecos.data.repository.AuthException
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.domain.logic.FieldError
import cl.oyl.frutossecos.domain.logic.RegisterForm
import cl.oyl.frutossecos.domain.logic.Validators
import cl.oyl.frutossecos.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val role: Role = Role.CLIENTE,
    val login: String = "",
    val password: String = "",
    val loginError: FieldError? = null,
    val passwordError: FieldError? = null,
    val authError: AuthError? = null,
    val loading: Boolean = false,
    val success: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onRole(role: Role) = _state.update { it.copy(role = role, authError = null) }
    fun onLogin(v: String) = _state.update { it.copy(login = v, loginError = null, authError = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, passwordError = null, authError = null) }

    fun submit() {
        val s = _state.value
        val loginErr = Validators.required(s.login)
        val passErr = Validators.required(s.password)
        if (loginErr != null || passErr != null) {
            _state.update { it.copy(loginError = loginErr, passwordError = passErr) }
            return
        }
        _state.update { it.copy(loading = true, authError = null) }
        viewModelScope.launch {
            try {
                auth.login(s.login, s.password, s.role)
                _state.update { it.copy(loading = false, success = true) }
            } catch (e: AuthException) {
                _state.update { it.copy(loading = false, authError = e.error, password = "") }
            }
        }
    }
}

data class RegisterUiState(
    val form: RegisterForm = RegisterForm(),
    val avatarId: Int? = 0,
    val errors: Map<String, FieldError> = emptyMap(),
    val authError: AuthError? = null,
    val loading: Boolean = false,
    val success: Boolean = false,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun update(field: String, transform: (RegisterForm) -> RegisterForm) =
        _state.update { it.copy(form = transform(it.form), errors = it.errors - field, authError = null) }

    fun onAvatar(id: Int) = _state.update { it.copy(avatarId = id) }

    fun submit() {
        val s = _state.value
        val errors = Validators.register(s.form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            try {
                auth.register(s.form, s.avatarId)
                _state.update { it.copy(loading = false, success = true) }
            } catch (e: AuthException) {
                _state.update { it.copy(loading = false, authError = e.error) }
            }
        }
    }
}
