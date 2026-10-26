package cl.oyl.frutossecos.core.di

import cl.oyl.frutossecos.BuildConfig
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.local.LocalAuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Elige la implementación de cada repositorio según BuildConfig.USE_REMOTE_REPOSITORIES.
 * En esta versión el flag es false: todo funciona en local con Room/SQLite.
 * Las implementaciones Remote se agregarán junto con el backend.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    private fun <T> select(local: T): T {
        check(!BuildConfig.USE_REMOTE_REPOSITORIES) { "Implementaciones Remote aún no disponibles en esta versión" }
        return local
    }

    @Provides fun auth(local: LocalAuthRepository): AuthRepository = select(local)
}
