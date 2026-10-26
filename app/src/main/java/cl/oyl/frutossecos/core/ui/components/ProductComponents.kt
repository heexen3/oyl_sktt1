package cl.oyl.frutossecos.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.oyl.frutossecos.R
import cl.oyl.frutossecos.core.ui.theme.OylBeigeDark
import cl.oyl.frutossecos.core.ui.theme.OylBrownContainer
import cl.oyl.frutossecos.core.ui.theme.OylBrownInk
import cl.oyl.frutossecos.core.util.CurrencyFormatter
import cl.oyl.frutossecos.domain.model.Product
import coil3.compose.AsyncImage

@Composable
fun PriceText(amount: Long, modifier: Modifier = Modifier) {
    Text(
        CurrencyFormatter.clp(amount),
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun StockChip(stock: Int, minStock: Int, modifier: Modifier = Modifier) {
    val (text, container, content) = when {
        stock <= 0 -> Triple(stringResource(R.string.stock_out), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        stock <= minStock -> Triple(stringResource(R.string.stock_low, stock), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
        else -> Triple(stringResource(R.string.stock_available, stock), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

/** Imagen del producto; si no hay foto muestra un degradado beige con la inicial. */
@Composable
fun ProductImage(name: String, imageUrl: String?, size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.size(size).clip(shape).background(Brush.linearGradient(listOf(OylBrownContainer, OylBeigeDark))),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Text(name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = OylBrownInk)
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(R.string.cd_product_image, name),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductImage(product.name, product.imageUrl, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(product.presentation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PriceText(product.price)
                StockChip(product.stock, product.minStock)
            }
            if (onAdd != null) {
                FilledIconButton(onClick = onAdd, enabled = !product.isOutOfStock) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_to_cart, product.name))
                }
            }
        }
    }
}

@Composable
fun ClickableCard(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) { content() }
}
