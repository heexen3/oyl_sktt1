package cl.oyl.frutossecos.feature.cart

import cl.oyl.frutossecos.core.ui.theme.OylDimens
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.EmptyState
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.MessageEffect
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.ProductImage
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.core.ui.components.StatusChip
import cl.oyl.frutossecos.core.ui.components.formatDate
import cl.oyl.frutossecos.core.util.CurrencyFormatter
import cl.oyl.frutossecos.domain.logic.OrderRules
import cl.oyl.frutossecos.domain.model.DeliveryMode

@Composable
fun CartScreen(onContinue: () -> Unit, onCatalog: () -> Unit, vm: CartViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    when {
        s.loading -> LoadingState()
        s.lines.isEmpty() -> EmptyState(
            stringResource(R.string.cart_empty),
            icon = Icons.Filled.ShoppingCart,
            hint = stringResource(R.string.cart_empty_hint),
            action = { PrimaryButton(stringResource(R.string.action_go_catalog), onCatalog) },
        )
        else -> Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(OylDimens.ListSpacing)) {
                items(s.lines, key = { it.productId }) { line ->
                    val issue = s.issues.firstOrNull { it.productId == line.productId }
                    Card(
                        modifier = Modifier.animateItem(),
                        colors = CardDefaults.cardColors(containerColor = if (issue != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Row(Modifier.padding(OylDimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
                            ProductImage(line.name, line.imageUrl, 56.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(line.name, style = MaterialTheme.typography.titleSmall)
                                Text(CurrencyFormatter.clp(line.unitPrice), style = MaterialTheme.typography.bodySmall)
                                Text(CurrencyFormatter.clp(line.subtotal), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                if (issue != null) Text(stringResource(R.string.cart_issue, issue.available), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { vm.decrement(line) }) { Icon(Icons.Filled.Remove, stringResource(R.string.cd_decrease)) }
                            Text("${line.quantity}", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { vm.increment(line) }, enabled = line.quantity < line.stock) { Icon(Icons.Filled.Add, stringResource(R.string.cd_increase)) }
                            IconButton(onClick = { vm.remove(line) }) { Icon(Icons.Filled.Delete, stringResource(R.string.cd_remove), tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryRow(stringResource(R.string.cart_subtotal), CurrencyFormatter.clp(s.subtotal), bold = true)
                Text(stringResource(R.string.cart_shipping_note, CurrencyFormatter.clp(OrderRules.FREE_SHIPPING_FROM)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.issues.isNotEmpty()) {
                    Text(stringResource(R.string.cart_has_issues), color = MaterialTheme.colorScheme.error)
                    SecondaryButton(stringResource(R.string.action_fix_quantities), vm::fixIssues)
                }
                PrimaryButton(stringResource(R.string.action_continue_purchase), onContinue, enabled = s.canContinue)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null)
        Text(value, fontWeight = if (bold) FontWeight.Bold else null, color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CheckoutScreen(onOrders: () -> Unit, onContinueShopping: () -> Unit, onBackToCart: () -> Unit, vm: CheckoutViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    val placed = s.placed
    when {
        s.loading -> LoadingState()
        placed != null -> {
            // Animación de éxito: el check "rebota" al aparecer.
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val scale by animateFloatAsState(if (shown) 1f else 0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "check")
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                Box(Modifier.size(120.dp).scale(scale).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(72.dp))
                }
                Text(stringResource(R.string.checkout_success), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(R.string.checkout_order_number, placed.number), style = MaterialTheme.typography.titleMedium)
                StatusChip(placed.status)
                Text(formatDate(placed.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.checkout_total, CurrencyFormatter.clp(placed.total)), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.checkout_success_note), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                PrimaryButton(stringResource(R.string.action_view_orders), onOrders)
                SecondaryButton(stringResource(R.string.action_continue_shopping), onContinueShopping)
            }
        }
        s.lines.isEmpty() -> EmptyState(stringResource(R.string.cart_empty), icon = Icons.Filled.ShoppingCart, hint = stringResource(R.string.cart_empty_hint))
        else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.checkout_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(OylDimens.CardPadding), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    s.lines.forEach { l ->
                        SummaryRow("${l.quantity} × ${l.name}", CurrencyFormatter.clp(l.subtotal))
                    }
                    HorizontalDivider()
                    SummaryRow(stringResource(R.string.cart_subtotal), CurrencyFormatter.clp(s.subtotal))
                    SummaryRow(stringResource(R.string.checkout_shipping), if (s.shipping == 0L) stringResource(R.string.checkout_free) else CurrencyFormatter.clp(s.shipping))
                    SummaryRow(stringResource(R.string.checkout_total_label), CurrencyFormatter.clp(s.total), bold = true)
                }
            }
            Text(stringResource(R.string.checkout_delivery), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DeliveryMode.entries.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().selectable(s.mode == m, onClick = { vm.onMode(m) }, role = Role.RadioButton).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = s.mode == m, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(if (m == DeliveryMode.RETIRO) R.string.delivery_pickup else R.string.delivery_shipping))
                        Text(
                            stringResource(if (m == DeliveryMode.RETIRO) R.string.delivery_pickup_note else R.string.delivery_shipping_note, CurrencyFormatter.clp(OrderRules.SHIPPING_COST)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            s.user?.let { u ->
                Text(stringResource(R.string.checkout_contact), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${u.name} · ${u.phone}\n${u.email}" + (u.address?.let { "\n$it" } ?: ""), style = MaterialTheme.typography.bodyMedium)
                if (s.mode == DeliveryMode.DESPACHO && u.address.isNullOrBlank()) {
                    Text(stringResource(R.string.checkout_need_address), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (s.issues.isNotEmpty()) {
                s.issues.forEach { Text(stringResource(R.string.checkout_issue_line, it.name, it.available), color = MaterialTheme.colorScheme.error) }
                SecondaryButton(stringResource(R.string.action_back_to_cart), onBackToCart)
            }
            Text(stringResource(R.string.checkout_validation_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryButton(
                stringResource(R.string.action_confirm_order), vm::confirm, loading = s.submitting,
                enabled = !(s.mode == DeliveryMode.DESPACHO && s.user?.address.isNullOrBlank()),
            )
        }
    }
}
