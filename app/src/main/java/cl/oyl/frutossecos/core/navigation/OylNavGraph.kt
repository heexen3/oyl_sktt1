package cl.oyl.frutossecos.core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.oyl.frutossecos.MainUiState
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.OylTopBar
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.feature.placeholder.PlaceholderScreen
import cl.oyl.frutossecos.feature.auth.LoginScreen
import cl.oyl.frutossecos.feature.auth.RegisterScreen
import cl.oyl.frutossecos.feature.auth.WelcomeScreen
import androidx.compose.runtime.remember

/** Navega a un destino de la barra inferior conservando el estado de cada pestaña. */
fun NavHostController.navigateTopLevel(screen: Screen) {
    navigate(screen.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun OylApp(
    state: MainUiState,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)
    val role = state.role
    val showBottomBar = role != null && bottomItemsFor(role).any { it.screen.route == currentRoute }
    // Se fija una sola vez: después la navegación se maneja con login/logout explícitos.
    val startDestination = remember { if (role != null) Screen.Home.route else Screen.Welcome.route }
    val goHome: () -> Unit = { navController.navigate(Screen.Home.route) { popUpTo(0) } }

    Scaffold(
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
        NavHost(navController, startDestination = startDestination, modifier = Modifier.padding(padding)) {
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
                PlaceholderScreen(stringResource(Screen.Home.titleRes), stringResource(R.string.placeholder_phase_3),
                    subtitle = role?.let { stringResource(R.string.current_role, stringResource(if (it == Role.ADMINISTRADOR) R.string.role_admin else R.string.role_client)) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Screen.entries.filter { it.requiresSession && it != Screen.Home && it.isAllowedFor(role) }.forEach { s ->
                            TextButton(onClick = {
                                navController.navigate(if (s == Screen.ProductDetail) Screen.productDetail(1) else s.route)
                            }) { Text(stringResource(R.string.placeholder_go_to, stringResource(s.titleRes))) }
                        }
                    }
                }
            }
            composable(Screen.Map.route) { Simple(Screen.Map, R.string.placeholder_phase_8) }
            composable(Screen.Catalog.route) { Simple(Screen.Catalog, R.string.placeholder_phase_3) }
            composable(
                Screen.ProductDetail.route,
                arguments = listOf(navArgument(Screen.ARG_PRODUCT_ID) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(Screen.ARG_PRODUCT_ID) ?: 0L
                PlaceholderScreen(stringResource(Screen.ProductDetail.titleRes), stringResource(R.string.placeholder_phase_3),
                    subtitle = stringResource(R.string.placeholder_product_id, id.toInt()))
            }
            composable(Screen.Scanner.route) { Simple(Screen.Scanner, R.string.placeholder_phase_5) }
            composable(Screen.Cart.route) { Simple(Screen.Cart, R.string.placeholder_phase_4) }
            composable(Screen.Checkout.route) { Simple(Screen.Checkout, R.string.placeholder_phase_4) }
            composable(Screen.AdminProducts.route) { Simple(Screen.AdminProducts, R.string.placeholder_phase_6) }
            composable(Screen.AdminInventory.route) { Simple(Screen.AdminInventory, R.string.placeholder_phase_6) }
            composable(Screen.Orders.route) { Simple(Screen.Orders, R.string.placeholder_phase_7) }
            composable(Screen.Notifications.route) { Simple(Screen.Notifications, R.string.placeholder_phase_7) }
            composable(Screen.Profile.route) {
                PlaceholderScreen(stringResource(Screen.Profile.titleRes), stringResource(R.string.placeholder_phase_8)) {
                    SecondaryButton(stringResource(R.string.action_logout), {
                        onLogout()
                        navController.navigate(Screen.Welcome.route) { popUpTo(0) }
                    })
                }
            }
        }
    }
}

@Composable
private fun Simple(screen: Screen, phaseRes: Int) {
    PlaceholderScreen(stringResource(screen.titleRes), stringResource(phaseRes))
}
