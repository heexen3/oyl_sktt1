package cl.oyl.frutossecos.core.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta OYL: verde oscuro + crema/beige + café (frutos secos), según las capturas del documento.
val OylGreen = Color(0xFF1B5E3A)
val OylGreenDark = Color(0xFF0F3D25)
val OylGreenLight = Color(0xFFA8D5B5)
val OylGreenContainer = Color(0xFFD3EBD9)
val OylCream = Color(0xFFFBF7EF)
val OylBeige = Color(0xFFF1E8D6)
val OylBeigeDark = Color(0xFFE2D5BC)
val OylBrown = Color(0xFF8B5E34)
val OylBrownContainer = Color(0xFFF3DEC8)
val OylInk = Color(0xFF1F2A22)
val OylMuted = Color(0xFF5E6A61)
val OylError = Color(0xFFC62828)
val OylErrorContainer = Color(0xFFFDE2E2)
val OylWarning = Color(0xFFE08A00)

// Colores de marca independientes del modo claro/oscuro: superficies "hero" verdes (bienvenida, banner de inicio)
// siempre llevan texto crema. Crema sobre #1B5E3A = 7.3:1 y sobre #0F3D25 = 11.6:1 (WCAG AA).
val OylOnBrand = OylCream
val OylOnBrandMuted = Color(0xFFE8F1EA) // texto secundario sobre verde (≥ 6.5:1)
val OylBrownInk = Color(0xFF6B4423)     // inicial sobre placeholder beige (≥ 6:1)
