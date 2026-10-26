package cl.oyl.frutossecos.domain

import cl.oyl.frutossecos.domain.logic.FieldError
import cl.oyl.frutossecos.domain.logic.RegisterForm
import cl.oyl.frutossecos.domain.logic.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun email() {
        assertNull(Validators.email("ana@correo.cl"))
        assertEquals(FieldError.INVALID_EMAIL, Validators.email("ana@correo"))
        assertEquals(FieldError.REQUIRED, Validators.email(" "))
    }

    @Test fun password_y_alias() {
        assertNull(Validators.password("clave1234"))
        assertEquals(FieldError.PASSWORD_TOO_SHORT, Validators.password("a1"))
        assertEquals(FieldError.PASSWORD_WEAK, Validators.password("soloLetras"))
        assertNull(Validators.alias("ana.p"))
        assertEquals(FieldError.ALIAS_INVALID, Validators.alias("a b"))
    }

    @Test fun telefono_chileno() {
        assertNull(Validators.phone("+56 9 1234 1234"))
        assertNull(Validators.phone("912341234"))
        assertEquals(FieldError.INVALID_PHONE, Validators.phone("1234"))
    }

    @Test fun formulario_registro_completo() {
        assertTrue(Validators.register(RegisterForm("Ana", "ana", "ana@correo.cl", "clave1234", "+56 9 1111 2222")).isEmpty())
        assertEquals(setOf("name", "alias", "email", "password", "phone"), Validators.register(RegisterForm()).keys)
    }
}
