package cl.oyl.frutossecos.core.ui.components

import cl.oyl.frutossecos.core.ui.theme.OylDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.util.CurrencyFormatter
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.Order
import cl.oyl.frutossecos.domain.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatDate(millis: Long): String = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("es-CL")).format(Date(millis))

@Composable
fun statusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.PENDIENTE_VALIDACION -> R.string.status_pending
        OrderStatus.CONFIRMADO -> R.string.status_confirmed
        OrderStatus.PREPARADO -> R.string.status_prepared
        OrderStatus.ENTREGADO -> R.string.status_delivered
        OrderStatus.CANCELADO -> R.string.status_cancelled
    }
)

@Composable
fun deliveryLabel(mode: DeliveryMode): String =
    stringResource(if (mode == DeliveryMode.RETIRO) R.string.delivery_pickup else R.string.delivery_shipping)

@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        OrderStatus.PENDIENTE_VALIDACION -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        OrderStatus.CONFIRMADO, OrderStatus.PREPARADO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.ENTREGADO -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        OrderStatus.CANCELADO -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(statusLabel(status), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
fun OrderCard(order: Order, onClick: () -> Unit, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(Modifier.padding(OylDimens.CardPadding), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(order.number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatDate(order.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(deliveryLabel(order.deliveryMode), style = MaterialTheme.typography.bodySmall, color = Color.Unspecified)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(CurrencyFormatter.clp(order.total), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                StatusChip(order.status)
            }
        }
    }
}
