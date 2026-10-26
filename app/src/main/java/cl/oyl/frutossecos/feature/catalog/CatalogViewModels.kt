package cl.oyl.frutossecos.feature.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.UiMessage
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.CartRepository
import cl.oyl.frutossecos.data.repository.ProductRepository
import cl.oyl.frutossecos.domain.model.Category
import cl.oyl.frutossecos.domain.model.Product
import cl.oyl.frutossecos.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val loading: Boolean = true,
    val query: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val role: Role? = null,
    val cartCount: Int = 0,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val products: ProductRepository,
    private val cart: CartRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<Long?>(null)
    val message = MutableStateFlow<UiMessage?>(null)

    private val results = combine(query.debounce(300), category) { q, c -> q to c }
        .flatMapLatest { (q, c) -> products.observeCatalog(q, c, onlyActive = true) }

    private val cartCount = auth.session.flatMapLatest { s ->
        if (s?.role == Role.CLIENTE) cart.observeCount(s.userId) else flowOf(0)
    }

    val state: StateFlow<CatalogUiState> = combine(
        query, category, products.observeCategories(), results, combine(auth.session, cartCount) { s, c -> s?.role to c },
    ) { q, c, cats, list, (role, count) ->
        CatalogUiState(false, q, c, cats, list, role, count)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun onQuery(v: String) { query.value = v }
    fun onCategory(id: Long?) { category.value = id }

    fun addToCart(product: Product) = viewModelScope.launch {
        val s = auth.session.first() ?: return@launch
        val before = cart.observeLines(s.userId).first().firstOrNull { it.productId == product.id }?.quantity ?: 0
        val after = cart.add(s.userId, product.id, 1)
        message.value = if (after <= before) UiMessage(R.string.msg_stock_limit, product.name)
        else UiMessage(R.string.msg_added_to_cart, product.name)
    }
}

data class ProductDetailUiState(
    val loading: Boolean = true,
    val product: Product? = null,
    val categoryName: String? = null,
    val quantity: Int = 1,
    val role: Role? = null,
    val inCart: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val products: ProductRepository,
    private val cart: CartRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val productId: Long = savedStateHandle.get<Long>("id") ?: 0L
    private val quantity = MutableStateFlow(1)
    val message = MutableStateFlow<UiMessage?>(null)

    private val inCart = auth.session.flatMapLatest { s ->
        if (s?.role == Role.CLIENTE) cart.observeLines(s.userId).map { l -> l.firstOrNull { it.productId == productId }?.quantity ?: 0 }
        else flowOf(0)
    }

    val state: StateFlow<ProductDetailUiState> = combine(
        products.observeProduct(productId), products.observeCategories(), quantity, auth.session, inCart,
    ) { p, cats, q, s, c ->
        val max = ((p?.stock ?: 0) - c).coerceAtLeast(0)
        ProductDetailUiState(false, p, cats.firstOrNull { it.id == p?.categoryId }?.name, q.coerceIn(if (max == 0) 0 else 1, maxOf(max, 1)), s?.role, c)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    fun maxAddable(): Int = ((state.value.product?.stock ?: 0) - state.value.inCart).coerceAtLeast(0)
    fun increment() = quantity.update { (it + 1).coerceAtMost(maxOf(maxAddable(), 1)) }
    fun decrement() = quantity.update { (it - 1).coerceAtLeast(1) }

    fun addToCart() = viewModelScope.launch {
        val s = auth.session.first() ?: return@launch
        val p = state.value.product ?: return@launch
        if (maxAddable() <= 0) { message.value = UiMessage(R.string.msg_stock_limit, p.name); return@launch }
        cart.add(s.userId, p.id, state.value.quantity)
        quantity.value = 1
        message.value = UiMessage(R.string.msg_added_to_cart, p.name)
    }
}
