package cl.oyl.frutossecos.feature.orders

import cl.oyl.frutossecos.core.ui.theme.OylDimens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.EmptyState
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.MessageEffect
import cl.oyl.frutossecos.core.ui.components.OrderCard
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.core.ui.components.StatusChip
import cl.oyl.frutossecos.core.ui.components.UiMessage
import cl.oyl.frutossecos.core.ui.components.deliveryLabel
import cl.oyl.frutossecos.core.ui.components.formatDate
import cl.oyl.frutossecos.core.ui.components.statusLabel
import cl.oyl.frutossecos.core.util.CurrencyFormatter
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.InvalidTransitionException
import cl.oyl.frutossecos.data.repository.OrderRepository
import cl.oyl.frutossecos.domain.logic.OrderRules
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.OrderItem
import cl.oyl.frutossecos.domain.model.OrderStatus
import cl.oyl.frutossecos.domain.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val loading: Boolean = true,
    val role: Role? = null,
    val status: OrderStatus? = null,
    val orders: List<Order> = emptyList(),
    val selected: Order? = null,
    val items: List<OrderItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrdersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    auth: AuthRepository,
    private val orders: OrderRepository,
) : ViewModel() {
    private val status = MutableStateFlow<OrderStatus?>(null)
    /** Pedido abierto en el detalle; se preselecciona si se llega con ?id= (por ejemplo, desde una notificación). */
    val selectedId = MutableStateFlow(savedStateHandle.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 })
    val message = MutableStateFlow<UiMessage?>(null)

    val state: StateFlow<OrdersUiState> = auth.session.flatMapLatest { s ->
        if (s == null) return@flatMapLatest flowOf(OrdersUiState(loading = false))
        val list = status.flatMapLatest { st -> orders.observeOrders(s.userId, s.role, st) }
        val detail = selectedId.flatMapLatest { id -> if (id == null) flowOf(null) else orders.observeOrder(id) }
        val items = selectedId.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else orders.observeItems(id) }
        combine(list, status, detail, items) { l, st, d, i ->
            // Un cliente solo puede ver el detalle de sus propios pedidos.
            val safe = d?.takeIf { s.role == Role.ADMINISTRADOR || it.userId == s.userId }
            OrdersUiState(false, s.role, st, l, safe, if (safe == null) emptyList() else i)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrdersUiState())

    fun onStatus(s: OrderStatus?) { status.value = s }
    fun open(id: Long) { selectedId.value = id }
    fun close() { selectedId.value = null }

    fun advance(order: Order) {
        val next = OrderRules.next(order.status) ?: return
        change(order, next)
    }
    fun cancel(order: Order) = change(order, OrderStatus.CANCELADO)

    private fun change(order: Order, to: OrderStatus) = viewModelScope.launch {
        try {
            orders.updateStatus(order.id, to)
            message.value = UiMessage(R.string.orders_status_updated)
        } catch (e: InvalidTransitionException) {
            message.value = UiMessage(R.string.orders_invalid_transition)
        }
    }
}

@Composable
fun OrdersScreen(vm: OrdersViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    if (s.loading) { LoadingState(); return }
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(s.status == null, { vm.onStatus(null) }, label = { Text(stringResource(R.string.category_all)) }) }
            items(OrderStatus.entries) { st -> FilterChip(s.status == st, { vm.onStatus(st) }, label = { Text(statusLabel(st)) }) }
        }
        if (s.orders.isEmpty()) EmptyState(stringResource(R.string.orders_empty), icon = Icons.AutoMirrored.Filled.ReceiptLong, hint = stringResource(R.string.orders_empty_hint))
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(OylDimens.ListSpacing)) {
            items(s.orders, key = { it.id }) { o ->
                OrderCard(o, onClick = { vm.open(o.id) }, modifier = Modifier.animateItem(),
                    highlight = s.role == Role.ADMINISTRADOR && o.status == OrderStatus.PENDIENTE_VALIDACION)
            }
        }
    }
    s.selected?.let { order -> OrderDetailSheet(order, s.items, s.role == Role.ADMINISTRADOR, vm) }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun OrderDetailSheet(order: Order, items: List<OrderItem>, isAdmin: Boolean, vm: OrdersViewModel) {
    var confirmCancel by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = vm::close) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(order.number, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(formatDate(order.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusChip(order.status)
            }
            Text(deliveryLabel(order.deliveryMode))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(OylDimens.CardPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEach { i ->
                        Row { Text("${i.quantity} × ${i.name}", Modifier.weight(1f)); Text(CurrencyFormatter.clp(i.subtotal)) }
                    }
                    HorizontalDivider()
                    Row { Text(stringResource(R.string.checkout_shipping), Modifier.weight(1f)); Text(CurrencyFormatter.clp(order.shipping)) }
                    Row {
                        Text(stringResource(R.string.checkout_total_label), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(CurrencyFormatter.clp(order.total), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (isAdmin) {
                OrderRules.next(order.status)?.let { next ->
                    PrimaryButton(stringResource(R.string.orders_advance_to, statusLabel(next)), { vm.advance(order) })
                }
            }
            if (OrderRules.canCancel(order.status)) {
                SecondaryButton(stringResource(R.string.orders_cancel), { confirmCancel = true })
            }
            TextButton(onClick = vm::close, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_close)) }
        }
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.orders_cancel_title)) },
            text = { Text(stringResource(R.string.orders_cancel_text, order.number)) },
            confirmButton = { TextButton(onClick = { confirmCancel = false; vm.cancel(order) }) { Text(stringResource(R.string.orders_cancel)) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.action_back)) } },
        )
    }
}
