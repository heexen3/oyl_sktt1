package cl.oyl.frutossecos.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.CartRepository
import cl.oyl.frutossecos.data.repository.NotificationRepository
import cl.oyl.frutossecos.data.repository.OrderRepository
import cl.oyl.frutossecos.data.repository.ProductRepository
import cl.oyl.frutossecos.data.repository.ProfileRepository
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.Product
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val user: User? = null,
    val role: Role? = null,
    val unread: Int = 0,
    val cartCount: Int = 0,
    val featured: List<Product> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val pendingOrders: Int = 0,
    val lowStock: List<Product> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    auth: AuthRepository,
    profile: ProfileRepository,
    products: ProductRepository,
    cart: CartRepository,
    orders: OrderRepository,
    notifications: NotificationRepository,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = auth.session.flatMapLatest { s ->
        if (s == null) return@flatMapLatest flowOf(HomeUiState(loading = false))
        val isAdmin = s.role == Role.ADMINISTRADOR
        val base = combine(profile.observeUser(s.userId), notifications.observeUnreadCount(s.userId), orders.observeOrders(s.userId, s.role)) { u, n, o ->
            HomeUiState(false, u, s.role, n, recentOrders = o.take(3))
        }
        if (isAdmin) {
            combine(base, orders.observePendingCount(), products.observeLowStock()) { b, pending, low ->
                b.copy(pendingOrders = pending, lowStock = low)
            }
        } else {
            combine(base, cart.observeCount(s.userId), products.observeCatalog().map { l -> l.filter { !it.isOutOfStock }.take(6) }) { b, c, f ->
                b.copy(cartCount = c, featured = f)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
