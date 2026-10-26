package cl.oyl.frutossecos.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.oyl.frutossecos.MainUiState
import cl.oyl.frutossecos.core.ui.components.LocalSnackbar
import cl.oyl.frutossecos.core.ui.components.OylTopBar
import cl.oyl.frutossecos.feature.admin.AdminInventoryScreen
import cl.oyl.frutossecos.feature.admin.AdminProductsScreen
import cl.oyl.frutossecos.feature.auth.LoginScreen
import cl.oyl.frutossecos.feature.auth.RegisterScreen
import cl.oyl.frutossecos.feature.auth.WelcomeScreen
import cl.oyl.frutossecos.feature.cart.CartScreen
import cl.oyl.frutossecos.feature.cart.CheckoutScreen
import cl.oyl.frutossecos.feature.catalog.CatalogScreen
import cl.oyl.frutossecos.feature.catalog.ProductDetailScreen
import cl.oyl.frutossecos.feature.home.HomeScreen
import cl.oyl.frutossecos.feature.map.MapScreen
import cl.oyl.frutossecos.feature.notifications.NotificationsScreen
import cl.oyl.frutossecos.feature.orders.OrdersScreen
import cl.oyl.frutossecos.feature.profile.ProfileScreen
import cl.oyl.frutossecos.feature.scanner.ScannerScreen

/** Navega a un destino de la barra inferior conservando el estado de cada pestaña. */
fun NavHostController.navigateTopLevel(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Ruta con argumento opcional ?id= (pedidos, inventario y productos admin). */
private fun withOptionalId(screen: Screen) = "${screen.route}?id={id}"
private val optionalId = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })

@Composable
fun OylApp(
    state: MainUiState,
    onLogout: () -> Unit,
    pendingRoute: String? = null,
    onPendingRouteHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route?.substringBefore('?')
    val currentScreen = Screen.fromRoute(currentRoute)
    val role = state.role
    val showBottomBar = role != null && bottomItemsFor(role).any { it.screen.route == currentRoute }
    // Se fija una sola vez: después la navegación se maneja con login/logout explícitos.
    val startDestination = remember { if (role != null) Screen.Home.route else Screen.Welcome.route }
    val goHome: () -> Unit = { navController.navigate(Screen.Home.route) { popUpTo(0) } }
    val go: (String) -> Unit = { route ->
        val target = Screen.fromRoute(route)
        if (target != null && target.isAllowedFor(role)) navController.navigate(route) { launchSingleTop = true }
    }
    val snackbar = remember { SnackbarHostState() }

    // Apertura desde una notificación del sistema (extra target_route).
    LaunchedEffect(pendingRoute, role) {
        if (pendingRoute != null && role != null) { go(pendingRoute); onPendingRouteHandled() }
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (currentScreen != null && currentScreen != Screen.Welcome) {
                    OylTopBar(
                        title = stringResource(currentScreen.titleRes),
                        onBack = if (showBottomBar) null else ({ navController.popBackStack() }),
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) OylBottomBar(role, currentRoute) { navController.navigateTopLevel(it) }
            },
        ) { padding ->
            NavHost(
                navController, startDestination = startDestination, modifier = Modifier.padding(padding),
                enterTransition = { fadeIn() }, exitTransition = { fadeOut() },
            ) {
                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onStart = { navController.navigate(Screen.Login.route) },
                        onLogin = { navController.navigate(Screen.Login.route) },
                        onRegister = { navController.navigate(Screen.Register.route) },
                    )
                }
                composable(Screen.Register.route) {
                    RegisterScreen(onRegistered = goHome, onLogin = {
                        navController.navigate(Screen.Login.route) { popUpTo(Screen.Welcome.route) }
                    })
                }
                composable(Screen.Login.route) {
                    LoginScreen(onLoggedIn = goHome, onRegister = {
                        navController.navigate(Screen.Register.route) { popUpTo(Screen.Welcome.route) }
                    })
                }
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigate = { r -> Screen.fromRoute(r)?.let { s -> if (bottomItemsFor(role ?: return@let).any { it.screen == s }) navController.navigateTopLevel(s) else go(r) } },
                        onProduct = { go(Screen.productDetail(it)) },
                        onOrder = { go("${Screen.Orders.route}?id=$it") },
                    )
                }
                composable(Screen.Map.route) { MapScreen() }
                composable(Screen.Catalog.route) {
                    CatalogScreen(onProduct = { go(Screen.productDetail(it)) }, onCart = { go(Screen.Cart.route) })
                }
                composable(
                    Screen.ProductDetail.route,
                    arguments = listOf(navArgument(Screen.ARG_PRODUCT_ID) { type = NavType.LongType }),
                ) {
                    ProductDetailScreen(
                        onEdit = { go("${Screen.AdminProducts.route}?id=$it") },
                        onCart = { go(Screen.Cart.route) },
                        onAdjust = { go("${Screen.AdminInventory.route}?id=$it") },
                    )
                }
                composable(Screen.Scanner.route) {
                    ScannerScreen(
                        onProduct = { go(Screen.productDetail(it)) },
                        onCart = { go(Screen.Cart.route) },
                        onEdit = { go("${Screen.AdminProducts.route}?id=$it") },
                        onAdjust = { go("${Screen.AdminInventory.route}?id=$it") },
                    )
                }
                composable(Screen.Cart.route) {
                    CartScreen(onContinue = { go(Screen.Checkout.route) }, onCatalog = { navController.navigateTopLevel(Screen.Catalog) })
                }
                composable(Screen.Checkout.route) {
                    CheckoutScreen(
                        onOrders = { navController.navigate(Screen.Orders.route) { popUpTo(Screen.Home.route) } },
                        onContinueShopping = { navController.navigate(Screen.Catalog.route) { popUpTo(Screen.Home.route) } },
                        onBackToCart = { navController.popBackStack() },
                    )
                }
                composable(withOptionalId(Screen.AdminProducts), arguments = optionalId) {
                    AdminProductsScreen(onAdjust = { go("${Screen.AdminInventory.route}?id=$it") })
                }
                composable(withOptionalId(Screen.AdminInventory), arguments = optionalId) { AdminInventoryScreen() }
                composable(withOptionalId(Screen.Orders), arguments = optionalId) { OrdersScreen() }
                composable(Screen.Notifications.route) { NotificationsScreen(onOpen = go) }
                composable(Screen.Profile.route) {
                    ProfileScreen(onLogout = {
                        onLogout()
                        navController.navigate(Screen.Welcome.route) { popUpTo(0) }
                    })
                }
            }
        }
    }
}
