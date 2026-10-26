package cl.oyl.frutossecos.data.repository.local

import cl.oyl.frutossecos.data.local.dao.SessionDao
import cl.oyl.frutossecos.data.remote.interceptor.TokenHolder
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.domain.model.Session
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class LocalAuthRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val tokenHolder: TokenHolder,
) : AuthRepository {
    override val session: Flow<Session?> = sessionDao.observe().map { e ->
        tokenHolder.token = e?.token
        e?.let { Session(it.userId, it.role, it.token, it.loggedAt) }
    }

    override suspend fun logout() = sessionDao.clear()
}
