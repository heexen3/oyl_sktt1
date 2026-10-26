package cl.oyl.frutossecos.domain

import cl.oyl.frutossecos.domain.logic.CartRules
import cl.oyl.frutossecos.domain.logic.OrderRules
import cl.oyl.frutossecos.domain.logic.QrParser
import cl.oyl.frutossecos.domain.logic.StoreHours
import cl.oyl.frutossecos.domain.model.CartLine
import cl.oyl.frutossecos.domain.model.DeliveryMode
import cl.oyl.frutossecos.domain.model.OrderStatus
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessRulesTest {
    @Test fun envio_gratis_desde_25000_y_retiro_sin_costo() {
        assertEquals(2_500L, OrderRules.shipping(10_000, DeliveryMode.DESPACHO))
        assertEquals(0L, OrderRules.shipping(25_000, DeliveryMode.DESPACHO))
        assertEquals(0L, OrderRules.shipping(1_000, DeliveryMode.RETIRO))
        assertEquals("OYL-000042", OrderRules.orderNumber(42))
    }

    @Test fun flujo_de_estados() {
        assertEquals(OrderStatus.CONFIRMADO, OrderRules.next(OrderStatus.PENDIENTE_VALIDACION))
        assertNull(OrderRules.next(OrderStatus.ENTREGADO))
        assertTrue(OrderRules.canCancel(OrderStatus.CONFIRMADO))
        assertFalse(OrderRules.canCancel(OrderStatus.PREPARADO))
        assertFalse(OrderRules.canTransition(OrderStatus.PENDIENTE_VALIDACION, OrderStatus.ENTREGADO))
    }

    @Test fun reglas_de_carrito() {
        assertEquals(3, CartRules.clamp(10, 3))
        val lines = listOf(CartLine(1, "Nuez", 1000, 5, 2, null), CartLine(2, "Maní", 500, 1, 9, null))
        assertEquals(1L, CartRules.issues(lines).single().productId)
        assertEquals(5_500L, OrderRules.subtotal(lines))
    }

    @Test fun qr_y_horario() {
        assertEquals(7L, QrParser.productId(" oyl-p-00000007 "))
        assertNull(QrParser.productId("https://otro.cl"))
        val h = "Lun a Sáb 09:00–20:00"
        assertEquals(true, StoreHours.isOpen(h, LocalDateTime.of(2026, 10, 26, 10, 0)))  // lunes
        assertEquals(false, StoreHours.isOpen(h, LocalDateTime.of(2026, 10, 25, 10, 0))) // domingo
        assertEquals(false, StoreHours.isOpen(h, LocalDateTime.of(2026, 10, 26, 21, 0)))
    }
}
