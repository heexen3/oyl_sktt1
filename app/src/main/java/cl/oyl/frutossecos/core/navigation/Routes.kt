package cl.oyl.frutossecos.core.navigation

import androidx.annotation.StringRes
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.domain.model.Role

/** Las 15 pantallas de la app con su ruta y los roles que pueden verla. */
enum class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val roles: Set<Role>,
    val requiresSession: Boolean = true,
) {
    Welcome("welcome", R.string.screen_welcome, Role.entries.toSet(), requiresSession = false),
    Register("register", R.string.screen_register, setOf(Role.CLIENTE), requiresSession = false),
    Login("login", R.string.screen_login, Role.entries.toSet(), requiresSession = false),
    Home("home", R.string.screen_home, Role.entries.toSet()),
    Map("map", R.string.screen_map, setOf(Role.CLIENTE)),
    Catalog("catalog", R.string.screen_catalog, Role.entries.toSet()),
    ProductDetail("product/{id}", R.string.screen_product_detail, Role.entries.toSet()),
    Scanner("scanner", R.string.screen_scanner, Role.entries.toSet()),
    Cart("cart", R.string.screen_cart, setOf(Role.CLIENTE)),
    Checkout("checkout", R.string.screen_checkout, setOf(Role.CLIENTE)),
    AdminProducts("admin/products", R.string.screen_admin_products, setOf(Role.ADMINISTRADOR)),
    AdminInventory("admin/inventory", R.string.screen_admin_inventory, setOf(Role.ADMINISTRADOR)),
    Orders("orders", R.string.screen_orders, Role.entries.toSet()),
    Notifications("notifications", R.string.screen_notifications, Role.entries.toSet()),
    Profile("profile", R.string.screen_profile, Role.entries.toSet());

    fun isAllowedFor(role: Role?): Boolean = if (!requiresSession) true else role != null && role in roles

    companion object {
        const val ARG_PRODUCT_ID = "id"
        const val ARG_PRESELECT_PRODUCT = "productId"
        fun productDetail(id: Long) = "product/$id"
        fun fromRoute(route: String?): Screen? {
            val base = route?.substringBefore('?') ?: return null
            return entries.firstOrNull { it.route == base }
                ?: entries.firstOrNull { it.route.contains('{') && base.startsWith(it.route.substringBefore('{')) }
        }
    }
}
