package cl.oyl.frutossecos.core.di

import cl.oyl.frutossecos.BuildConfig
import cl.oyl.frutossecos.core.notifications.AndroidSystemNotifier
import cl.oyl.frutossecos.core.notifications.SystemNotifier
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.CartRepository
import cl.oyl.frutossecos.data.repository.InventoryRepository
import cl.oyl.frutossecos.data.repository.NotificationRepository
import cl.oyl.frutossecos.data.repository.OrderRepository
import cl.oyl.frutossecos.data.repository.ProductRepository
import cl.oyl.frutossecos.data.repository.ProfileRepository
import cl.oyl.frutossecos.data.repository.StoreRepository
import cl.oyl.frutossecos.data.repository.local.LocalAuthRepository
import cl.oyl.frutossecos.data.repository.local.LocalCartRepository
import cl.oyl.frutossecos.data.repository.local.LocalInventoryRepository
import cl.oyl.frutossecos.data.repository.local.LocalNotificationRepository
import cl.oyl.frutossecos.data.repository.local.LocalOrderRepository
import cl.oyl.frutossecos.data.repository.local.LocalProductRepository
import cl.oyl.frutossecos.data.repository.local.LocalProfileRepository
import cl.oyl.frutossecos.data.repository.local.LocalStoreRepository
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
    @Provides fun products(local: LocalProductRepository): ProductRepository = select(local)
    @Provides fun cart(local: LocalCartRepository): CartRepository = select(local)
    @Provides fun orders(local: LocalOrderRepository): OrderRepository = select(local)
    @Provides fun inventory(local: LocalInventoryRepository): InventoryRepository = select(local)
    @Provides fun stores(local: LocalStoreRepository): StoreRepository = select(local)
    @Provides fun notifications(local: LocalNotificationRepository): NotificationRepository = select(local)
    @Provides fun profile(local: LocalProfileRepository): ProfileRepository = select(local)
    @Provides fun systemNotifier(impl: AndroidSystemNotifier): SystemNotifier = impl
}
