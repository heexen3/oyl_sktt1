package cl.oyl.frutossecos.feature.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.EmptyState
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.MessageEffect
import cl.oyl.frutossecos.core.ui.components.OylTextField
import cl.oyl.frutossecos.core.ui.components.PriceText
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.ProductImage
import cl.oyl.frutossecos.core.ui.components.StockChip
import cl.oyl.frutossecos.domain.model.MovementType
import cl.oyl.frutossecos.domain.model.Product

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, singleLine = true,
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        placeholder = { Text(stringResource(R.string.catalog_search)) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
fun AdminProductsScreen(onAdjust: (Long) -> Unit, vm: AdminProductsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    if (s.loading) { LoadingState(); return }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SearchField(s.query, vm::onQuery)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ProductFilter.entries) { f ->
                    FilterChip(s.filter == f, { vm.onFilter(f) }, label = {
                        Text(stringResource(when (f) {
                            ProductFilter.TODOS -> R.string.category_all
                            ProductFilter.ACTIVOS -> R.string.admin_filter_active
                            ProductFilter.INACTIVOS -> R.string.admin_filter_inactive
                            ProductFilter.STOCK_BAJO -> R.string.admin_filter_low
                        }))
                    })
                }
            }
            if (s.products.isEmpty()) EmptyState(stringResource(R.string.catalog_empty))
            else LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.products, key = { it.id }) { p ->
                    Card(
                        onClick = { vm.edit(p) }, modifier = Modifier.animateItem(),
                        colors = CardDefaults.cardColors(containerColor = if (p.active) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProductImage(p.name, p.imageUrl, 56.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall)
                                PriceText(p.price)
                                StockChip(p.stock, p.minStock)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Switch(checked = p.active, onCheckedChange = { vm.toggleActive(p) })
                                Text(stringResource(if (p.active) R.string.admin_active else R.string.admin_inactive), style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { onAdjust(p.id) }) { Icon(Icons.Filled.Edit, stringResource(R.string.action_adjust_inventory)) }
                        }
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = vm::create, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text(stringResource(R.string.admin_new_product)) },
        )
    }
    s.form?.let { ProductFormSheet(it, s, vm) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductFormSheet(f: ProductForm, s: AdminProductsUiState, vm: AdminProductsViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::onImagePicked) }
    ModalBottomSheet(onDismissRequest = vm::dismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(if (f.id == null) R.string.admin_new_product else R.string.action_edit_product), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProductImage(f.name.ifBlank { "?" }, f.imagePreview, 88.dp)
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.admin_pick_photo))
                }
            }
            OylTextField(f.name, { v -> vm.update { it.copy(name = v) } }, stringResource(R.string.admin_field_name), error = f.errors["name"])
            Text(stringResource(R.string.admin_field_category), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.categories, key = { it.id }) { c -> FilterChip(f.categoryId == c.id, { vm.update { it.copy(categoryId = c.id) } }, label = { Text(c.name) }) }
            }
            if (f.errors["category"] != null) Text(stringResource(R.string.error_required), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OylTextField(f.description, { v -> vm.update { it.copy(description = v) } }, stringResource(R.string.admin_field_description), singleLine = false)
            OylTextField(f.price, { v -> vm.update { it.copy(price = v.filter(Char::isDigit)) } }, stringResource(R.string.admin_field_price), error = f.errors["price"], keyboardType = KeyboardType.Number)
            OylTextField(f.presentation, { v -> vm.update { it.copy(presentation = v) } }, stringResource(R.string.admin_field_presentation), error = f.errors["presentation"])
            OylTextField(f.weightGrams, { v -> vm.update { it.copy(weightGrams = v.filter(Char::isDigit)) } }, stringResource(R.string.admin_field_weight), error = f.errors["weight"], keyboardType = KeyboardType.Number)
            OylTextField(f.origin, { v -> vm.update { it.copy(origin = v) } }, stringResource(R.string.admin_field_origin))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OylTextField(f.stock, { v -> vm.update { it.copy(stock = v.filter(Char::isDigit)) } }, stringResource(R.string.admin_field_stock), Modifier.weight(1f), error = f.errors["stock"], keyboardType = KeyboardType.Number)
                OylTextField(f.minStock, { v -> vm.update { it.copy(minStock = v.filter(Char::isDigit)) } }, stringResource(R.string.admin_field_min_stock), Modifier.weight(1f), error = f.errors["minStock"], keyboardType = KeyboardType.Number)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.admin_active), Modifier.weight(1f))
                Switch(checked = f.active, onCheckedChange = { v -> vm.update { it.copy(active = v) } })
            }
            PrimaryButton(stringResource(R.string.action_save), vm::save, loading = f.saving)
            TextButton(onClick = vm::dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_cancel)) }
        }
    }
}

@Composable
fun AdminInventoryScreen(vm: AdminInventoryViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    if (s.loading) { LoadingState(); return }
    Column(Modifier.fillMaxSize()) {
        SearchField(s.query, vm::onQuery)
        if (s.pendingSync > 0) {
            Text(stringResource(R.string.inventory_pending_sync, s.pendingSync), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 16.dp))
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.products, key = { it.id }) { p ->
                Card(onClick = { vm.open(p) }, modifier = Modifier.animateItem(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.inventory_stock_line, p.stock, p.minStock), style = MaterialTheme.typography.bodySmall)
                        }
                        StockChip(p.stock, p.minStock)
                    }
                }
            }
        }
    }
    s.adjusting?.let { AdjustDialog(it, vm) }
}

@Composable
private fun AdjustDialog(p: Product, vm: AdminInventoryViewModel) {
    var type by remember(p.id) { mutableStateOf(MovementType.ENTRADA) }
    var qty by remember(p.id) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = vm::dismiss,
        title = { Text(p.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.inventory_current, p.stock))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MovementType.entries.forEachIndexed { i, t ->
                        SegmentedButton(type == t, { type = t }, SegmentedButtonDefaults.itemShape(i, MovementType.entries.size)) {
                            Text(stringResource(if (t == MovementType.ENTRADA) R.string.inventory_in else R.string.inventory_out))
                        }
                    }
                }
                OutlinedTextField(qty, { qty = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.product_quantity)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                qty.toIntOrNull()?.let { q ->
                    val result = if (type == MovementType.ENTRADA) p.stock + q else p.stock - q
                    Text(stringResource(R.string.inventory_result, result), color = if (result < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = { vm.adjust(p, type, qty) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = vm::dismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
