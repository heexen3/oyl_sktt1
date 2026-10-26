package cl.oyl.frutossecos.data.repository

import cl.oyl.frutossecos.data.local.DbTestBase
import cl.oyl.frutossecos.data.local.seed.SeedData
import cl.oyl.frutossecos.data.remote.interceptor.TokenHolder
import cl.oyl.frutossecos.data.repository.local.LocalAuthRepository
import cl.oyl.frutossecos.domain.logic.RegisterForm
import cl.oyl.frutossecos.domain.model.Role
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryTest : DbTestBase() {
    private fun repo() = LocalAuthRepository(db, TokenHolder())
    private val form = RegisterForm("Ana Pérez", "ana", "Ana@Correo.cl", "clave1234", "+56 9 1111 2222", "")

    @Test fun admin_demo_puede_iniciar_sesion_con_alias_y_correo() = runTest {
        seed()
        val r = repo()
        assertEquals(Role.ADMINISTRADOR, r.login("admin", SeedData.DEMO_ADMIN_PASSWORD, Role.ADMINISTRADOR).role)
        assertEquals(Role.ADMINISTRADOR, r.login("admin@admin.cl", SeedData.DEMO_ADMIN_PASSWORD, Role.ADMINISTRADOR).role)
        assertEquals(Role.ADMINISTRADOR, r.session.first()?.role)
    }

    @Test fun credenciales_invalidas_o_rol_equivocado_fallan_con_mensaje_generico() = runTest {
        seed()
        val r = repo()
        for ((login, pass, role) in listOf(
            Triple("admin", "malaClave1", Role.ADMINISTRADOR),
            Triple("admin", SeedData.DEMO_ADMIN_PASSWORD, Role.CLIENTE),
            Triple("noexiste", "x", Role.CLIENTE),
        )) {
            try { r.login(login, pass, role); fail("debió fallar") } catch (e: AuthException) {
                assertEquals(AuthError.INVALID_CREDENTIALS, e.error)
            }
        }
        assertNull(r.currentSession())
    }

    @Test fun registro_crea_cliente_con_hash_inicia_sesion_y_rechaza_duplicados() = runTest {
        seed()
        val r = repo()
        val user = r.register(form, avatarId = 2)
        assertEquals(Role.CLIENTE, user.role)
        assertEquals("ana@correo.cl", user.email)
        val stored = db.userDao().getById(user.id)!!
        assertNotEquals("clave1234", stored.passwordHash)
        assertEquals(user.id, r.currentSession()?.userId)
        r.logout()
        assertNull(r.currentSession())
        assertEquals(user.id, r.login("ANA", "clave1234", Role.CLIENTE).id)
        try { r.register(form.copy(email = "otra@correo.cl"), null); fail() } catch (e: AuthException) { assertEquals(AuthError.ALIAS_TAKEN, e.error) }
        try { r.register(form.copy(alias = "ana2"), null); fail() } catch (e: AuthException) { assertEquals(AuthError.EMAIL_TAKEN, e.error) }
    }
}
