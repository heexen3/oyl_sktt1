package cl.oyl.frutossecos.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.AvatarPicker
import cl.oyl.frutossecos.core.ui.components.OylTextField
import cl.oyl.frutossecos.core.ui.components.PasswordField
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.core.ui.theme.OylGreen
import cl.oyl.frutossecos.core.ui.theme.OylGreenDark
import cl.oyl.frutossecos.core.ui.theme.OylCream
import cl.oyl.frutossecos.core.ui.theme.OylOnBrand
import cl.oyl.frutossecos.core.ui.theme.OylOnBrandMuted
import cl.oyl.frutossecos.data.repository.AuthError
import cl.oyl.frutossecos.domain.model.Role

@Composable
fun authErrorText(e: AuthError?): String? = when (e) {
    null -> null
    AuthError.INVALID_CREDENTIALS -> stringResource(R.string.error_invalid_credentials)
    AuthError.ALIAS_TAKEN -> stringResource(R.string.error_alias_taken)
    AuthError.EMAIL_TAKEN -> stringResource(R.string.error_email_taken)
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit, onRegister: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(OylGreenDark, OylGreen))).systemBarsPadding(),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { -it / 3 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(112.dp).background(OylCream, CircleShape), contentAlignment = Alignment.Center) {
                        Text("🌰", style = MaterialTheme.typography.displayMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = OylOnBrand, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.brand_tagline), style = MaterialTheme.typography.titleMedium, color = OylOnBrandMuted, textAlign = TextAlign.Center)
                }
            }
            AnimatedVisibility(visible, enter = fadeIn(tween(900, delayMillis = 300))) {
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.extraLarge).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.welcome_intro), style = MaterialTheme.typography.bodyLarge)
                    Feature(Icons.Filled.ShoppingBasket, stringResource(R.string.welcome_feature_catalog))
                    Feature(Icons.Filled.QrCodeScanner, stringResource(R.string.welcome_feature_qr))
                    Feature(Icons.Filled.LocationOn, stringResource(R.string.welcome_feature_map))
                    Feature(Icons.Filled.Inventory2, stringResource(R.string.welcome_feature_orders))
                    Spacer(Modifier.height(4.dp))
                    PrimaryButton(stringResource(R.string.action_start), onStart)
                    SecondaryButton(stringResource(R.string.action_have_account), onLogin)
                    TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.action_create_account))
                    }
                }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onRegister: () -> Unit, vm: LoginViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.success) { if (s.success) onLoggedIn() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.login_heading), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SecondaryTabRow(selectedTabIndex = if (s.role == Role.CLIENTE) 0 else 1) {
            Tab(s.role == Role.CLIENTE, onClick = { vm.onRole(Role.CLIENTE) }, text = { Text(stringResource(R.string.role_client)) })
            Tab(s.role == Role.ADMINISTRADOR, onClick = { vm.onRole(Role.ADMINISTRADOR) }, text = { Text(stringResource(R.string.role_admin)) })
        }
        OylTextField(s.login, vm::onLogin, stringResource(R.string.field_alias_or_email), error = s.loginError, keyboardType = KeyboardType.Email)
        PasswordField(s.password, vm::onPassword, stringResource(R.string.field_password), error = s.passwordError)
        authErrorText(s.authError)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        if (s.role == Role.ADMINISTRADOR) {
            Text(stringResource(R.string.login_admin_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        PrimaryButton(stringResource(R.string.action_login), vm::submit, loading = s.loading)
        if (s.role == Role.CLIENTE) {
            TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.login_no_account))
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onLogin: () -> Unit, vm: RegisterViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.success) { if (s.success) onRegistered() }
    val f = s.form
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.register_heading), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.register_avatar), style = MaterialTheme.typography.titleSmall)
        AvatarPicker(s.avatarId, vm::onAvatar)
        OylTextField(f.name, { v -> vm.update("name") { it.copy(name = v) } }, stringResource(R.string.field_name), error = s.errors["name"])
        OylTextField(f.alias, { v -> vm.update("alias") { it.copy(alias = v) } }, stringResource(R.string.field_alias), error = s.errors["alias"], supporting = stringResource(R.string.field_alias_help))
        OylTextField(f.email, { v -> vm.update("email") { it.copy(email = v) } }, stringResource(R.string.field_email), error = s.errors["email"], keyboardType = KeyboardType.Email)
        PasswordField(f.password, { v -> vm.update("password") { it.copy(password = v) } }, stringResource(R.string.field_password), error = s.errors["password"])
        OylTextField(f.phone, { v -> vm.update("phone") { it.copy(phone = v) } }, stringResource(R.string.field_phone), error = s.errors["phone"], keyboardType = KeyboardType.Phone, supporting = stringResource(R.string.field_phone_help))
        OylTextField(f.address, { v -> vm.update("address") { it.copy(address = v) } }, stringResource(R.string.field_address_optional))
        authErrorText(s.authError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(stringResource(R.string.action_register), vm::submit, loading = s.loading)
        TextButton(onClick = onLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.action_have_account)) }
    }
}
