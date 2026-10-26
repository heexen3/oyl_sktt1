package cl.oyl.frutossecos.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

/** Avatares de demostración (sin fotos reales). */
object Avatars {
    val all = listOf(
        "🌰" to Color(0xFFF3DEC8), "🥜" to Color(0xFFD3EBD9), "🌻" to Color(0xFFFFF1C2),
        "🫐" to Color(0xFFDCE3FA), "🍇" to Color(0xFFEAD9F2), "🌱" to Color(0xFFE2F2D0),
    )
}

@Composable
fun OylAvatar(avatarId: Int?, avatarUri: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val avatar = avatarId?.let { Avatars.all.getOrNull(it) }
    Box(
        modifier.size(size).clip(CircleShape).background(avatar?.second ?: MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !avatarUri.isNullOrBlank() -> AsyncImage(avatarUri, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
            avatar != null -> Text(avatar.first, fontSize = (size.value * 0.5f).sp)
            else -> Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun AvatarPicker(selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatars.all.forEachIndexed { index, _ ->
            val border = if (selected == index) MaterialTheme.colorScheme.primary else Color.Transparent
            OylAvatar(index, null, "", 52.dp,
                Modifier.border(3.dp, border, CircleShape).clip(CircleShape).clickable { onSelect(index) })
        }
    }
}
