package cl.oyl.frutossecos.data.repository

import cl.oyl.frutossecos.domain.logic.RegisterForm
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.domain.model.Session
import cl.oyl.frutossecos.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Sesión y autenticación. Implementaciones: Local (Room) ahora, Remote (JWT backend) más adelante. */
interface AuthRepository {
    val session: Flow<Session?>
    /** Registra un Cliente e inicia sesión. Lanza [AuthException] si alias/correo ya existen. */
    suspend fun register(form: RegisterForm, avatarId: Int?): User
    /** Lanza [AuthException] con INVALID_CREDENTIALS si no coincide (mensaje genérico). */
    suspend fun login(login: String, password: String, role: Role): User
    suspend fun currentSession(): Session?
    suspend fun logout()
}
