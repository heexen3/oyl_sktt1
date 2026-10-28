package cl.oyl.frutossecos.feature.home

import cl.oyl.frutossecos.core.ui.theme.OylDimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.navigation.Screen
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.OrderCard
import cl.oyl.frutossecos.core.ui.components.OylAvatar
import cl.oyl.frutossecos.core.ui.components.PriceText
import cl.oyl.frutossecos.core.ui.components.ProductImage
import cl.oyl.frutossecos.core.ui.components.SectionHeader
import cl.oyl.frutossecos.core.ui.components.StockChip
import cl.oyl.frutossecos.core.ui.theme.OylGreen
import cl.oyl.frutossecos.core.ui.theme.OylGreenDark
import cl.oyl.frutossecos.core.ui.theme.OylOnBrand
import cl.oyl.frutossecos.core.ui.theme.OylOnBrandMuted
import cl.oyl.frutossecos.domain.model.Role

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onProduct: (Long) -> Unit,
    onOrder: (Long) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    if (s.loading) { LoadingState(); return }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val isAdmin = s.role == Role.ADMINISTRADOR
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Encabezado con saludo, avatar y campana de notificaciones
        Row(verticalAlignment = Alignment.CenterVertically) {
            s.user?.let { OylAvatar(it.avatarId, it.avatarUri, it.name, 48.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_greeting, s.user?.name?.substringBefore(' ') ?: ""), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(if (isAdmin) R.string.role_admin else R.string.role_client), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = { onNavigate(Screen.Notifications.route) }) {
                BadgedBox(badge = { if (s.unread > 0) Badge { Text("${s.unread}") } }) {
                    Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.screen_notifications))
                }
            }
        }
        AnimatedVisibility(visible, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }) {
            Column(
                Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(OylGreenDark, OylGreen)), MaterialTheme.shapes.large).padding(20.dp),
            ) {
                Text(stringResource(if (isAdmin) R.string.home_hero_admin else R.string.home_hero_client), style = MaterialTheme.typography.titleMedium, color = OylOnBrand, fontWeight = FontWeight.Bold)
                Text(
                    if (isAdmin) stringResource(R.string.home_hero_admin_sub, s.pendingOrders, s.lowStock.size)
                    else stringResource(R.string.home_hero_client_sub),
                    style = MaterialTheme.typography.bodyMedium, color = OylOnBrandMuted,
                )
            }
        }
        SectionHeader(stringResource(R.string.home_quick_access))
        val shortcuts = if (isAdmin) listOf(
            Triple(Icons.Filled.Inventory2, R.string.screen_admin_products, Screen.AdminProducts.route),
            Triple(Icons.Filled.Inventory, R.string.screen_admin_inventory, Screen.AdminInventory.route),
            Triple(Icons.AutoMirrored.Filled.ReceiptLong, R.string.screen_orders, Screen.Orders.route),
            Triple(Icons.Filled.QrCodeScanner, R.string.screen_scanner, Screen.Scanner.route),
            Triple(Icons.Filled.Storefront, R.string.screen_catalog, Screen.Catalog.route),
            Triple(Icons.Filled.Notifications, R.string.screen_notifications, Screen.Notifications.route),
        ) else listOf(
            Triple(Icons.Filled.Storefront, R.string.screen_catalog, Screen.Catalog.route),
            Triple(Icons.Filled.ShoppingCart, R.string.screen_cart, Screen.Cart.route),
            Triple(Icons.Filled.QrCodeScanner, R.string.screen_scanner, Screen.Scanner.route),
            Triple(Icons.Filled.LocationOn, R.string.screen_map, Screen.Map.route),
            Triple(Icons.AutoMirrored.Filled.ReceiptLong, R.string.screen_orders, Screen.Orders.route),
            Triple(Icons.Filled.Notifications, R.string.screen_notifications, Screen.Notifications.route),
        )
        shortcuts.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (icon, label, route) ->
                    val badge = if (route == Screen.Cart.route) s.cartCount else if (route == Screen.Orders.route && isAdmin) s.pendingOrders else 0
                    QuickTile(icon, stringResource(label), badge, Modifier.weight(1f)) { onNavigate(route) }
                }
            }
        }
        if (isAdmin) {
            SectionHeader(stringResource(R.string.home_low_stock))
            if (s.lowStock.isEmpty()) Text(stringResource(R.string.home_low_stock_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            s.lowStock.take(5).forEach { p ->
                Card(onClick = { onProduct(p.id) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(OylDimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        StockChip(p.stock, p.minStock)
                    }
                }
            }
        } else {
            SectionHeader(stringResource(R.string.home_featured))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.featured, key = { it.id }) { p ->
                    Card(onClick = { onProduct(p.id) }, modifier = Modifier.width(150.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ProductImage(p.name, p.imageUrl, 130.dp)
                            Text(p.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            PriceText(p.price)
                        }
                    }
                }
            }
        }
        SectionHeader(stringResource(R.string.home_recent_orders))
        if (s.recentOrders.isEmpty()) Text(stringResource(R.string.orders_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.recentOrders.forEach { o -> OrderCard(o, onClick = { onOrder(o.id) }) }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, badge: Int, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BadgedBox(badge = { if (badge > 0) Badge { Text("$badge") } }) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
