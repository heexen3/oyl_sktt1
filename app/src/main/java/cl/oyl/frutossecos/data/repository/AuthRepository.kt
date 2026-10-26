package cl.oyl.frutossecos.data.repository

import cl.oyl.frutossecos.domain.model.Session
import kotlinx.coroutines.flow.Flow

/** Sesión y autenticación. Implementaciones: Local (Room) ahora, Remote (JWT backend) más adelante. */
interface AuthRepository {
    val session: Flow<Session?>
    suspend fun logout()
}
