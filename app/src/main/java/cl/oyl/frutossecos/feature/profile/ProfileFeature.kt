package cl.oyl.frutossecos.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.AvatarPicker
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.MessageEffect
import cl.oyl.frutossecos.core.ui.components.OylAvatar
import cl.oyl.frutossecos.core.ui.components.OylTextField
import cl.oyl.frutossecos.core.ui.components.PrimaryButton
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.core.ui.components.SectionHeader
import cl.oyl.frutossecos.core.ui.components.UiMessage
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.ProfileRepository
import cl.oyl.frutossecos.domain.logic.FieldError
import cl.oyl.frutossecos.domain.logic.Validators
import cl.oyl.frutossecos.domain.model.Role
import cl.oyl.frutossecos.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    auth: AuthRepository,
    private val profile: ProfileRepository,
) : ViewModel() {
    val user: StateFlow<User?> = auth.session.flatMapLatest { s -> if (s == null) flowOf(null) else profile.observeUser(s.userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val errors = MutableStateFlow<Map<String, FieldError>>(emptyMap())
    val message = MutableStateFlow<UiMessage?>(null)

    fun save(base: User, name: String, phone: String, address: String, avatarId: Int?) {
        val e = buildMap {
            Validators.required(name)?.let { put("name", it) }
            Validators.phone(phone)?.let { put("phone", it) }
        }
        errors.value = e
        if (e.isNotEmpty()) return
        viewModelScope.launch {
            profile.update(base.copy(name = name, phone = phone, address = address, avatarId = avatarId))
            message.value = UiMessage(R.string.profile_saved)
        }
    }

    fun setNotifications(base: User, enabled: Boolean) = viewModelScope.launch { profile.update(base.copy(notificationsEnabled = enabled)) }
}

@Composable
fun ProfileScreen(onLogout: () -> Unit, vm: ProfileViewModel = hiltViewModel()) {
    val user by vm.user.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    MessageEffect(msg) { vm.message.value = null }
    val u = user ?: run { LoadingState(); return }
    val context = LocalContext.current
    var name by remember(u.id) { mutableStateOf(u.name) }
    var phone by remember(u.id) { mutableStateOf(u.phone) }
    var address by remember(u.id) { mutableStateOf(u.address.orEmpty()) }
    var avatar by remember(u.id) { mutableStateOf(u.avatarId) }
    var confirmLogout by remember { mutableStateOf(false) }
    var perms by remember { mutableStateOf(permissionStatus(context)) }
    LifecycleResumeEffect(Unit) { perms = permissionStatus(context); onPauseOrDispose { } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OylAvatar(avatar, u.avatarUri, name, 72.dp)
            Column(Modifier.padding(start = 14.dp)) {
                Text(u.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("@${u.alias} · ${u.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(if (u.role == Role.ADMINISTRADOR) R.string.role_admin else R.string.role_client), color = MaterialTheme.colorScheme.secondary)
            }
        }
        SectionHeader(stringResource(R.string.profile_avatar))
        AvatarPicker(avatar, { avatar = it })
        SectionHeader(stringResource(R.string.profile_data))
        OylTextField(name, { name = it }, stringResource(R.string.field_name), error = errors["name"])
        OylTextField(phone, { phone = it }, stringResource(R.string.field_phone), error = errors["phone"], keyboardType = KeyboardType.Phone)
        OylTextField(address, { address = it }, stringResource(R.string.field_address_optional))
        PrimaryButton(stringResource(R.string.action_save), { vm.save(u, name, phone, address, avatar) })

        SectionHeader(stringResource(R.string.profile_notifications))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_notifications_switch), Modifier.weight(1f))
            Switch(checked = u.notificationsEnabled, onCheckedChange = { vm.setNotifications(u, it) })
        }
        SectionHeader(stringResource(R.string.profile_permissions))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                perms.forEach { (label, ok) ->
                    Row {
                        Text(stringResource(label), Modifier.weight(1f))
                        Text(stringResource(if (ok) R.string.profile_perm_granted else R.string.profile_perm_denied),
                            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }) { Text(stringResource(R.string.profile_open_settings)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        SecondaryButton(stringResource(R.string.action_logout), { confirmLogout = true })
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.action_logout)) },
            text = { Text(stringResource(R.string.profile_logout_confirm)) },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text(stringResource(R.string.action_logout)) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun permissionStatus(context: android.content.Context): List<Pair<Int, Boolean>> {
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    return listOf(
        R.string.perm_camera to granted(Manifest.permission.CAMERA),
        R.string.perm_location to (granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION)),
        R.string.perm_notifications to (Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)),
    )
}
