package cl.oyl.backend

import com.jayway.jsonpath.JsonPath
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApiIntegrationTest(@Autowired private val mvc: MockMvc) {

    private fun MockHttpServletRequestBuilder.json(body: String, token: String? = null) =
        contentType(MediaType.APPLICATION_JSON).content(body).also { r -> token?.let { r.header("Authorization", "Bearer $it") } }

    private fun MockHttpServletRequestBuilder.auth(token: String) = header("Authorization", "Bearer $token")

    private fun login(login: String, password: String, role: String): String =
        JsonPath.read(mvc.perform(post("/api/v1/auth/login").json("""{"login":"$login","password":"$password","role":"$role"}"""))
            .andExpect(status().isOk).andReturn().response.contentAsString, "$.token")

    private fun registerClient(alias: String, address: String? = null): String =
        JsonPath.read(mvc.perform(post("/api/v1/auth/register").json(
            """{"name":"Cliente $alias","alias":"$alias","email":"$alias@correo.cl","password":"clave1234","phone":"+56 9 1111 2222","address":${address?.let { "\"$it\"" } ?: "null"}}"""))
            .andExpect(status().isCreated).andReturn().response.contentAsString, "$.token")

    @Test
    fun `catalogo publico y login de admin demo con rol`() {
        mvc.perform(get("/api/v1/products")).andExpect(status().isOk).andExpect(jsonPath("$", hasSize<Any>(11)))
        mvc.perform(get("/api/v1/products/by-qr/OYL-P-0001")).andExpect(jsonPath("$.name").value("Almendras"))
        mvc.perform(get("/api/v1/stores")).andExpect(jsonPath("$", hasSize<Any>(3)))
        login("admin", "OylAdmin2026!", "ADMINISTRADOR")
        login("admin@admin.cl", "OylAdmin2026!", "ADMINISTRADOR")
        mvc.perform(post("/api/v1/auth/login").json("""{"login":"admin","password":"OylAdmin2026!","role":"CLIENTE"}"""))
            .andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    fun `registro valida datos y rechaza duplicados`() {
        registerClient("ana")
        mvc.perform(post("/api/v1/auth/register").json("""{"name":"Otra","alias":"ana","email":"otra@correo.cl","password":"clave1234","phone":"+56 9 1111 2222"}"""))
            .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("ALIAS_TAKEN"))
        mvc.perform(post("/api/v1/auth/register").json("""{"name":"X","alias":"a","email":"malo","password":"123","phone":"123"}"""))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `seguridad por rol`() {
        val client = registerClient("beto")
        mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized)
        mvc.perform(get("/api/v1/inventory").auth(client)).andExpect(status().isForbidden)
        mvc.perform(patch("/api/v1/orders/1/status").json("""{"status":"CONFIRMADO"}""", client)).andExpect(status().isForbidden)
        val admin = login("admin", "OylAdmin2026!", "ADMINISTRADOR")
        mvc.perform(get("/api/v1/inventory?lowStock=true").auth(admin)).andExpect(status().isOk)
            .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.containsInAnyOrder("Castañas de cajú", "Linaza")))
    }

    @Test
    fun `pedido descuenta stock, 409 sin stock, aislamiento entre clientes y cambio de estado notifica`() {
        val ana = registerClient("ana")
        val beto = registerClient("beto")
        val admin = login("admin", "OylAdmin2026!", "ADMINISTRADOR")
        mvc.perform(post("/api/v1/cart/validate").json("""{"items":[{"productId":5,"quantity":10}]}""", ana))
            .andExpect(status().isOk).andExpect(jsonPath("$.ok").value(false)).andExpect(jsonPath("$.issues[0].available").value(4))
        mvc.perform(post("/api/v1/orders").json("""{"items":[{"productId":1,"quantity":2},{"productId":8,"quantity":1}],"deliveryMode":"RETIRO"}""", ana))
            .andExpect(status().isConflict).andExpect(jsonPath("$.code").value("STOCK_INSUFICIENTE"))
            .andExpect(jsonPath("$.details[0].productId").value(8))
        mvc.perform(get("/api/v1/products/1")).andExpect(jsonPath("$.stock").value(25))
        mvc.perform(post("/api/v1/orders").json("""{"items":[{"productId":1,"quantity":2}],"deliveryMode":"DESPACHO"}""", ana))
            .andExpect(status().isBadRequest).andExpect(jsonPath("$.code").value("ADDRESS_REQUIRED"))
        val body = mvc.perform(post("/api/v1/orders").json("""{"items":[{"productId":1,"quantity":2}],"deliveryMode":"RETIRO"}""", ana))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDIENTE_VALIDACION"))
            .andExpect(jsonPath("$.total").value(5980))
            .andReturn().response.contentAsString
        val id: Int = JsonPath.read(body, "$.id")
        mvc.perform(get("/api/v1/products/1")).andExpect(jsonPath("$.stock").value(23))
        mvc.perform(get("/api/v1/orders").auth(beto)).andExpect(jsonPath("$", hasSize<Any>(0)))
        mvc.perform(get("/api/v1/orders/$id").auth(beto)).andExpect(status().isNotFound)
        mvc.perform(get("/api/v1/orders").auth(admin)).andExpect(jsonPath("$", hasSize<Any>(1)))
        mvc.perform(get("/api/v1/notifications").auth(admin)).andExpect(jsonPath("$[0].type").value("NUEVO_PEDIDO"))
        mvc.perform(patch("/api/v1/orders/$id/status").json("""{"status":"ENTREGADO"}""", admin)).andExpect(status().isConflict)
        mvc.perform(patch("/api/v1/orders/$id/status").json("""{"status":"CONFIRMADO"}""", admin)).andExpect(jsonPath("$.status").value("CONFIRMADO"))
        mvc.perform(get("/api/v1/notifications").auth(ana)).andExpect(jsonPath("$", hasSize<Any>(2)))
        mvc.perform(patch("/api/v1/orders/$id/status").json("""{"status":"CANCELADO"}""", admin)).andExpect(status().isOk)
        mvc.perform(get("/api/v1/products/1")).andExpect(jsonPath("$.stock").value(25))
    }

    @Test
    fun `movimientos de inventario son idempotentes por localId`() {
        val admin = login("admin", "OylAdmin2026!", "ADMINISTRADOR")
        val mov = """{"localId":"6f1c-uuid-1","productId":2,"type":"ENTRADA","quantity":5}"""
        mvc.perform(post("/api/v1/inventory/movements").json(mov, admin)).andExpect(status().isCreated).andExpect(jsonPath("$.resultingStock").value(23))
        mvc.perform(post("/api/v1/inventory/movements").json(mov, admin)).andExpect(status().isOk).andExpect(jsonPath("$.duplicated").value(true))
        mvc.perform(get("/api/v1/products/2")).andExpect(jsonPath("$.stock").value(23))
        mvc.perform(post("/api/v1/inventory/movements").json("""{"localId":"x2","productId":2,"type":"SALIDA","quantity":999}""", admin))
            .andExpect(status().isConflict)
    }

    @Test
    fun `admin crea, edita y desactiva productos`() {
        val admin = login("admin", "OylAdmin2026!", "ADMINISTRADOR")
        val req = """{"name":"Pistachos","categoryId":1,"description":"Pistachos tostados","price":5990,"presentation":"Bolsa 200 g","weightGrams":200,"origin":"Irán","stock":10,"minStock":3}"""
        val id: Int = JsonPath.read(mvc.perform(post("/api/v1/products").json(req, admin)).andExpect(status().isCreated)
            .andExpect(jsonPath("$.qrCode").value(org.hamcrest.Matchers.startsWith("OYL-P-"))).andReturn().response.contentAsString, "$.id")
        mvc.perform(patch("/api/v1/products/$id/active").json("""{"active":false}""", admin)).andExpect(jsonPath("$.active").value(false))
        mvc.perform(post("/api/v1/products").json(req.replace("5990", "0"), admin)).andExpect(status().isBadRequest)
    }
}
