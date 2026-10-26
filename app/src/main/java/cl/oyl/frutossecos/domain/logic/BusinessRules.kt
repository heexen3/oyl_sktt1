package cl.oyl.frutossecos.domain.logic

import cl.oyl.frutossecos.domain.model.CartLine
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.OrderStatus
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/** Reglas de negocio puras. */
object OrderRules {
    /** Costo de despacho referencial (CLP). Retiro en tienda es gratis; despacho gratis sobre el umbral. */
    const val SHIPPING_COST = 2_500L
    const val FREE_SHIPPING_FROM = 25_000L

    fun subtotal(lines: List<CartLine>): Long = lines.sumOf { it.subtotal }

    fun shipping(subtotal: Long, mode: DeliveryMode): Long = when {
        mode == DeliveryMode.RETIRO -> 0
        subtotal >= FREE_SHIPPING_FROM -> 0
        else -> SHIPPING_COST
    }

    fun orderNumber(id: Long): String = "OYL-%06d".format(id)

    /** Siguiente estado en el flujo del Administrador (null si es final). */
    fun next(status: OrderStatus): OrderStatus? = when (status) {
        OrderStatus.PENDIENTE_VALIDACION -> OrderStatus.CONFIRMADO
        OrderStatus.CONFIRMADO -> OrderStatus.PREPARADO
        OrderStatus.PREPARADO -> OrderStatus.ENTREGADO
        OrderStatus.ENTREGADO, OrderStatus.CANCELADO -> null
    }

    fun canCancel(status: OrderStatus): Boolean =
        status == OrderStatus.PENDIENTE_VALIDACION || status == OrderStatus.CONFIRMADO

    fun canTransition(from: OrderStatus, to: OrderStatus): Boolean =
        next(from) == to || (to == OrderStatus.CANCELADO && canCancel(from))
}

/** Problema de stock detectado al validar el carrito. */
data class StockIssue(val productId: Long, val name: String, val requested: Int, val available: Int)

object CartRules {
    /** Cantidad máxima agregable según stock disponible. */
    fun clamp(requested: Int, stock: Int): Int = requested.coerceIn(0, stock.coerceAtLeast(0))

    fun issues(lines: List<CartLine>): List<StockIssue> = lines
        .filter { it.quantity > it.stock }
        .map { StockIssue(it.productId, it.name, it.quantity, it.stock.coerceAtLeast(0)) }
}

/** Interpreta códigos QR de producto: "OYL-P-0001" (también acepta minúsculas/espacios). */
object QrParser {
    private val PATTERN = Regex("^OYL-P-(\\d{1,8})$")

    fun normalize(raw: String): String? {
        val code = raw.trim().uppercase()
        return if (PATTERN.matches(code)) code else null
    }

    fun productId(raw: String): Long? = normalize(raw)?.let { PATTERN.find(it)?.groupValues?.get(1)?.toLongOrNull() }
}

/** Horario de tiendas del tipo "Lun a Sáb 09:00–20:00". */
object StoreHours {
    private val PATTERN = Regex("(Lun) a (Vie|Sáb|Dom)\\s+(\\d{2}):(\\d{2})[–-](\\d{2}):(\\d{2})")

    fun isOpen(hours: String, at: LocalDateTime): Boolean? {
        val m = PATTERN.find(hours) ?: return null
        val lastDay = when (m.groupValues[2]) {
            "Vie" -> DayOfWeek.FRIDAY
            "Sáb" -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
        if (at.dayOfWeek > lastDay) return false
        val open = LocalTime.of(m.groupValues[3].toInt(), m.groupValues[4].toInt())
        val close = LocalTime.of(m.groupValues[5].toInt(), m.groupValues[6].toInt())
        val t = at.toLocalTime()
        return !t.isBefore(open) && t.isBefore(close)
    }
}
