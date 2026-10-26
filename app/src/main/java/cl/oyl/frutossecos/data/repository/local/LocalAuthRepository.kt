package cl.oyl.frutossecos.data.repository.local

import cl.oyl.frutossecos.core.util.PasswordHasher
import cl.oyl.frutossecos.data.local.OylDatabase
import cl.oyl.frutossecos.data.local.entity.SessionEntity
import cl.oyl.frutossecos.data.local.entity.UserEntity
import cl.oyl.frutossecos.data.local.mapper.toDomain
import cl.oyl.frutossecos.data.remote.interceptor.TokenHolder
import cl.oyl.frutossecos.data.repository.AuthError
import cl.oyl.frutossecos.data.repository.AuthException
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.domain.logic.RegisterForm
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.domain.model.Session
import cl.oyl.frutossecos.domain.model.User
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Autenticación local: usuarios en Room con hash PBKDF2 + sal. El "token" local es un identificador
 * aleatorio; con el backend será el JWT entregado por /auth/login.
 */
@Singleton
class LocalAuthRepository @Inject constructor(
    private val db: OylDatabase,
    private val tokenHolder: TokenHolder,
) : AuthRepository {
    private val sessionDao = db.sessionDao()
    private val userDao = db.userDao()

    override val session: Flow<Session?> = sessionDao.observe().map { e ->
        tokenHolder.token = e?.token
        e?.let { Session(it.userId, it.role, it.token, it.loggedAt) }
    }

    override suspend fun currentSession(): Session? =
        sessionDao.get()?.let { Session(it.userId, it.role, it.token, it.loggedAt) }

    override suspend fun register(form: RegisterForm, avatarId: Int?): User = withContext(Dispatchers.Default) {
        val alias = form.alias.trim()
        val email = form.email.trim().lowercase()
        if (userDao.findByAlias(alias) != null) throw AuthException(AuthError.ALIAS_TAKEN)
        if (userDao.findByEmail(email) != null) throw AuthException(AuthError.EMAIL_TAKEN)
        val hashed = PasswordHasher.hash(form.password)
        val entity = UserEntity(
            name = form.name.trim(), alias = alias, email = email, phone = form.phone.trim(),
            address = form.address.trim().ifBlank { null }, avatarId = avatarId,
            passwordHash = hashed.hash, salt = hashed.salt, role = Role.CLIENTE,
        )
        val id = userDao.insert(entity)
        startSession(id, Role.CLIENTE)
        entity.copy(id = id).toDomain()
    }

    override suspend fun login(login: String, password: String, role: Role): User = withContext(Dispatchers.Default) {
        val user = userDao.findForLogin(login.trim(), role)
        // Mensaje genérico: no se revela si el usuario existe.
        if (user == null || !PasswordHasher.verify(password, user.passwordHash, user.salt)) {
            throw AuthException(AuthError.INVALID_CREDENTIALS)
        }
        startSession(user.id, user.role)
        user.toDomain()
    }

    private suspend fun startSession(userId: Long, role: Role) {
        sessionDao.save(SessionEntity(userId = userId, role = role, token = "local-" + UUID.randomUUID(), loggedAt = System.currentTimeMillis()))
    }

    override suspend fun logout() = sessionDao.clear()
}
