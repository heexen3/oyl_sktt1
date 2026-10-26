package cl.oyl.frutossecos.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilTest {
    @Test fun formateaClp() {
        assertEquals("$12.470", CurrencyFormatter.clp(12470))
        assertEquals("$0", CurrencyFormatter.clp(0))
        assertEquals("$990", CurrencyFormatter.clp(990))
        assertEquals("$1.000.000", CurrencyFormatter.clp(1_000_000L))
    }

    @Test fun hashUsaSalYVerifica() {
        val a = PasswordHasher.hash("secreta")
        val b = PasswordHasher.hash("secreta")
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.hash, b.hash)
        assertTrue(PasswordHasher.verify("secreta", a.hash, a.salt))
        assertFalse(PasswordHasher.verify("Secreta", a.hash, a.salt))
    }
}
