package cl.oyl.frutossecos.feature.catalog

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.EmptyState
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.MessageEffect
import cl.oyl.frutossecos.core.ui.components.PriceText
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.ProductCard
import cl.oyl.frutossecos.core.ui.components.ProductImage
import cl.oyl.frutossecos.core.ui.components.StockChip
import cl.oyl.frutossecos.domain.model.Role

@Composable
fun CatalogScreen(onProduct: (Long) -> Unit, onCart: () -> Unit, vm: CatalogViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = s.query,
                onValueChange = vm::onQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.catalog_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(s.categoryId == null, { vm.onCategory(null) }, label = { Text(stringResource(R.string.category_all)) }) }
                items(s.categories, key = { it.id }) { c ->
                    FilterChip(s.categoryId == c.id, { vm.onCategory(c.id) }, label = { Text(c.name) })
                }
            }
            when {
                s.loading -> LoadingState()
                s.products.isEmpty() -> EmptyState(stringResource(R.string.catalog_empty))
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(s.products, key = { it.id }) { p ->
                        ProductCard(p, onClick = { onProduct(p.id) }, modifier = Modifier.animateItem(),
                            onAdd = if (s.role == Role.CLIENTE) ({ vm.addToCart(p) }) else null)
                    }
                }
            }
        }
        if (s.role == Role.CLIENTE) {
            ExtendedFloatingActionButton(
                onClick = onCart,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = {
                    BadgedBox(badge = { if (s.cartCount > 0) Badge { Text("${s.cartCount}") } }) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                    }
                },
                text = { Text(stringResource(R.string.screen_cart)) },
            )
        }
    }
}

@Composable
fun ProductDetailScreen(onEdit: (Long) -> Unit, onCart: () -> Unit, onAdjust: (Long) -> Unit, vm: ProductDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    val p = s.product
    when {
        s.loading -> LoadingState()
        p == null -> EmptyState(stringResource(R.string.product_not_found))
        else -> Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductImage(p.name, p.imageUrl, 260.dp, Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    s.categoryName?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary) }
                }
                PriceText(p.price)
            }
            StockChip(p.stock, p.minStock)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p.description, style = MaterialTheme.typography.bodyLarge)
                    InfoRow(stringResource(R.string.product_presentation), p.presentation)
                    InfoRow(stringResource(R.string.product_weight), stringResource(R.string.product_grams, p.weightGrams))
                    InfoRow(stringResource(R.string.product_origin), p.origin)
                    InfoRow(stringResource(R.string.product_qr), p.qrCode)
                }
            }
            if (s.role == Role.CLIENTE) {
                if (s.inCart > 0) Text(stringResource(R.string.product_in_cart, s.inCart), color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.product_quantity), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = vm::decrement, enabled = s.quantity > 1) { Icon(Icons.Filled.Remove, stringResource(R.string.cd_decrease)) }
                    Text("${s.quantity}", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = vm::increment, enabled = s.quantity < (p.stock - s.inCart)) { Icon(Icons.Filled.Add, stringResource(R.string.cd_increase)) }
                }
                PrimaryButton(stringResource(R.string.action_add_to_cart), vm::addToCart, enabled = p.stock - s.inCart > 0)
                if (s.inCart > 0) {
                    cl.oyl.frutossecos.core.ui.components.SecondaryButton(stringResource(R.string.action_go_to_cart), onCart)
                }
            } else if (s.role == Role.ADMINISTRADOR) {
                PrimaryButton(stringResource(R.string.action_edit_product), { onEdit(p.id) })
                cl.oyl.frutossecos.core.ui.components.SecondaryButton(stringResource(R.string.action_adjust_inventory), { onAdjust(p.id) })
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
