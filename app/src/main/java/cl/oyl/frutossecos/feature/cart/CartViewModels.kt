package cl.oyl.frutossecos.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.UiMessage
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.CartRepository
import cl.oyl.frutossecos.data.repository.EmptyCartException
import cl.oyl.frutossecos.data.repository.OrderRepository
import cl.oyl.frutossecos.data.repository.ProfileRepository
import cl.oyl.frutossecos.data.repository.StockException
import cl.oyl.frutossecos.domain.logic.CartRules
import cl.oyl.frutossecos.domain.logic.OrderRules
import cl.oyl.frutossecos.domain.logic.StockIssue
import cl.oyl.frutossecos.domain.model.CartLine
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(
    val loading: Boolean = true,
    val lines: List<CartLine> = emptyList(),
    val issues: List<StockIssue> = emptyList(),
) {
    val subtotal: Long get() = OrderRules.subtotal(lines)
    val canContinue: Boolean get() = lines.isNotEmpty() && issues.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CartViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val cart: CartRepository,
) : ViewModel() {
    val state: StateFlow<CartUiState> = auth.session.flatMapLatest { s ->
        if (s == null) flowOf(emptyList()) else cart.observeLines(s.userId)
    }.let { lines ->
        combine(lines, flowOf(Unit)) { l, _ -> CartUiState(false, l, CartRules.issues(l)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    private suspend fun uid() = auth.session.first()?.userId

    fun increment(line: CartLine) = viewModelScope.launch { uid()?.let { cart.add(it, line.productId, 1) } }
    fun decrement(line: CartLine) = viewModelScope.launch { uid()?.let { cart.setQuantity(it, line.productId, line.quantity - 1) } }
    fun remove(line: CartLine) = viewModelScope.launch { uid()?.let { cart.remove(it, line.productId) } }
    /** Ajusta las líneas con problemas al stock disponible. */
    fun fixIssues() = viewModelScope.launch {
        val id = uid() ?: return@launch
        state.value.issues.forEach { cart.setQuantity(id, it.productId, it.available) }
    }
}

data class CheckoutUiState(
    val loading: Boolean = true,
    val lines: List<CartLine> = emptyList(),
    val user: User? = null,
    val mode: DeliveryMode = DeliveryMode.RETIRO,
    val submitting: Boolean = false,
    val placed: Order? = null,
    val issues: List<StockIssue> = emptyList(),
) {
    val subtotal: Long get() = OrderRules.subtotal(lines)
    val shipping: Long get() = OrderRules.shipping(subtotal, mode)
    val total: Long get() = subtotal + shipping
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val auth: AuthRepository,
    cart: CartRepository,
    profile: ProfileRepository,
    private val orders: OrderRepository,
) : ViewModel() {
    private val mode = MutableStateFlow(DeliveryMode.RETIRO)
    private val submit = MutableStateFlow(SubmitState())
    val message = MutableStateFlow<UiMessage?>(null)

    private data class SubmitState(val submitting: Boolean = false, val placed: Order? = null, val issues: List<StockIssue> = emptyList())

    val state: StateFlow<CheckoutUiState> = auth.session.flatMapLatest { s ->
        if (s == null) flowOf(CheckoutUiState(loading = false))
        else combine(cart.observeLines(s.userId), profile.observeUser(s.userId), mode, submit) { l, u, m, sub ->
            CheckoutUiState(false, l, u, m, sub.submitting, sub.placed, sub.issues)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutUiState())

    fun onMode(m: DeliveryMode) { mode.value = m }

    fun confirm() {
        if (submit.value.submitting || submit.value.placed != null) return
        submit.value = SubmitState(submitting = true)
        viewModelScope.launch {
            val s = auth.session.first() ?: return@launch
            try {
                val order = orders.placeOrder(s.userId, mode.value)
                submit.value = SubmitState(placed = order)
            } catch (e: StockException) {
                submit.value = SubmitState(issues = e.issues)
                message.value = UiMessage(R.string.checkout_stock_error)
            } catch (e: EmptyCartException) {
                submit.value = SubmitState()
                message.value = UiMessage(R.string.cart_empty)
            }
        }
    }
}
