package cl.oyl.frutossecos.feature.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.components.EmptyState
import cl.oyl.frutossecos.core.ui.components.LoadingState
import cl.oyl.frutossecos.core.ui.components.SecondaryButton
import cl.oyl.frutossecos.core.ui.components.formatDate
import cl.oyl.frutossecos.data.repository.AuthRepository
import cl.oyl.frutossecos.data.repository.NotificationRepository
import cl.oyl.frutossecos.domain.model.AppNotification
import cl.oyl.frutossecos.domain.model.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(val loading: Boolean = true, val items: List<AppNotification> = emptyList())

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repo: NotificationRepository,
) : ViewModel() {
    val state: StateFlow<NotificationsUiState> = auth.session.flatMapLatest { s ->
        if (s == null) flowOf(NotificationsUiState(false)) else repo.observe(s.userId).map { NotificationsUiState(false, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    fun markRead(n: AppNotification) = viewModelScope.launch { auth.session.first()?.let { repo.markRead(it.userId, n.id) } }
    fun markAll() = viewModelScope.launch { auth.session.first()?.let { repo.markAllRead(it.userId) } }
}

/** Ruta de destino de una notificación (con ?id= si corresponde). */
fun AppNotification.destination(): String? = targetRoute?.let { r -> if (targetId != null && !r.contains('?')) "$r?id=$targetId" else r }

@Composable
fun NotificationsScreen(onOpen: (String) -> Unit, vm: NotificationsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (s.loading) { LoadingState(); return }
    Column(Modifier.fillMaxSize()) {
        if (!granted) {
            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.notif_permission_rationale))
                    SecondaryButton(stringResource(R.string.notif_permission_allow), { if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) })
                }
            }
        }
        if (s.items.any { !it.read }) {
            TextButton(onClick = vm::markAll, modifier = Modifier.align(Alignment.End).padding(end = 8.dp)) { Text(stringResource(R.string.notif_mark_all)) }
        }
        if (s.items.isEmpty()) EmptyState(stringResource(R.string.notif_empty), icon = Icons.Filled.NotificationsNone, hint = stringResource(R.string.notif_empty_hint))
        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.items, key = { it.id }) { n ->
                Card(
                    onClick = { vm.markRead(n); n.destination()?.let(onOpen) },
                    modifier = Modifier.animateItem(),
                    colors = CardDefaults.cardColors(containerColor = if (n.read) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            when (n.type) {
                                NotificationType.NUEVO_PEDIDO -> Icons.Filled.ReceiptLong
                                NotificationType.CAMBIO_ESTADO -> Icons.Filled.Sync
                                NotificationType.STOCK_BAJO -> Icons.Filled.Inventory
                                NotificationType.GENERAL -> Icons.Filled.Notifications
                            },
                            contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, fontWeight = if (n.read) FontWeight.Normal else FontWeight.Bold)
                            Text(n.message, style = MaterialTheme.typography.bodyMedium)
                            Text(formatDate(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
