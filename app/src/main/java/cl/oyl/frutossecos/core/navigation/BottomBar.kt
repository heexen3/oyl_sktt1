package cl.oyl.frutossecos.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.domain.model.Role

data class BottomItem(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val icon: ImageVector? = null,
    @DrawableRes val iconRes: Int? = null,
)

/** Ítems de la barra inferior según el rol (lógica pura, probada en tests). */
fun bottomItemsFor(role: Role?): List<BottomItem> = when (role) {
    Role.CLIENTE -> listOf(
        BottomItem(Screen.Home, R.string.nav_home, icon = Icons.Filled.Home),
        BottomItem(Screen.Map, R.string.nav_map, iconRes = R.drawable.ic_map),
        BottomItem(Screen.Scanner, R.string.nav_scanner, iconRes = R.drawable.ic_qr_scanner),
        BottomItem(Screen.Orders, R.string.nav_orders, iconRes = R.drawable.ic_receipt),
        BottomItem(Screen.Profile, R.string.nav_profile, icon = Icons.Filled.Person),
    )
    Role.ADMINISTRADOR -> listOf(
        BottomItem(Screen.Home, R.string.nav_home, icon = Icons.Filled.Home),
        BottomItem(Screen.AdminInventory, R.string.nav_inventory, iconRes = R.drawable.ic_inventory),
        BottomItem(Screen.Orders, R.string.nav_orders, iconRes = R.drawable.ic_receipt),
        BottomItem(Screen.Profile, R.string.nav_profile, icon = Icons.Filled.Person),
    )
    null -> emptyList()
}

@Composable
fun OylBottomBar(role: Role?, currentRoute: String?, onNavigate: (Screen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        bottomItemsFor(role).forEach { item ->
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick = { onNavigate(item.screen) },
                icon = {
                    when {
                        item.icon != null -> Icon(item.icon, contentDescription = label)
                        item.iconRes != null -> Icon(painterResource(item.iconRes), contentDescription = label)
                    }
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}
