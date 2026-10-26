package cl.oyl.backend.config

import cl.oyl.backend.domain.Category
import cl.oyl.backend.domain.Product
import cl.oyl.backend.domain.Role
import cl.oyl.backend.domain.Store
import cl.oyl.backend.domain.StoreType
import cl.oyl.backend.domain.UserAccount
import cl.oyl.backend.repo.CategoryRepository
import cl.oyl.backend.repo.ProductRepository
import cl.oyl.backend.repo.StoreRepository
import cl.oyl.backend.repo.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** Datos ficticios de demostración (los mismos que siembra la app en Room). */
@Component
class Seeder(
    private val users: UserRepository,
    private val categories: CategoryRepository,
    private val products: ProductRepository,
    private val stores: StoreRepository,
    private val encoder: PasswordEncoder,
    @Value("\${oyl.demo-admin-password}") private val adminPassword: String,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (users.count() > 0) return
        users.save(UserAccount("Administrador", "admin", "admin@admin.cl", "+56 9 1234 1234", "Santiago", null,
            encoder.encode(adminPassword)!!, Role.ADMINISTRADOR))
        val (frutos, semillas, deshidratados) = listOf("Frutos secos", "Semillas", "Deshidratados").map { categories.save(Category(it)) }
        listOf(
            Store("OYL Frutos Secos – Tienda Providencia", "Av. Providencia 1234, Providencia, Santiago", -33.4263, -70.6170, "Lun a Sáb 09:00–20:00", StoreType.TIENDA),
            Store("Punto de retiro Ñuñoa", "Irarrázaval 3456, Ñuñoa, Santiago", -33.4545, -70.5980, "Lun a Vie 10:00–19:00", StoreType.RETIRO),
            Store("Punto de retiro Santiago Centro", "Huérfanos 789, Santiago Centro", -33.4389, -70.6510, "Lun a Vie 09:30–18:30", StoreType.RETIRO),
        ).forEach { stores.save(it) }
        listOf(
            Product("Almendras", frutos, "Almendras enteras naturales, sin sal.", 2990, "Bolsa 250 g", 250, "Chile", null, 25, 5),
            Product("Nueces", frutos, "Nueces mariposa extra light.", 3490, "Bolsa 250 g", 250, "Chile", null, 18, 5),
            Product("Mix Premium", frutos, "Mezcla de almendras, nueces, castañas de cajú y pasas.", 3990, "Bolsa 250 g", 250, "Chile / Brasil", null, 12, 5),
            Product("Maní tostado", frutos, "Maní tostado sin sal.", 1490, "Bolsa 250 g", 250, "Argentina", null, 40, 10),
            Product("Castañas de cajú", frutos, "Castañas de cajú enteras.", 4490, "Bolsa 250 g", 250, "Brasil", null, 4, 5),
            Product("Semillas de chía", semillas, "Chía orgánica, fuente de omega 3.", 1990, "Bolsa 200 g", 200, "Bolivia", null, 30, 8),
            Product("Semillas de maravilla", semillas, "Maravilla pelada tostada.", 1590, "Bolsa 250 g", 250, "Chile", null, 22, 8),
            Product("Linaza", semillas, "Linaza dorada entera.", 1290, "Bolsa 250 g", 250, "Chile", null, 0, 5),
            Product("Pasas", deshidratados, "Pasas morenas sin semilla.", 1790, "Bolsa 250 g", 250, "Chile", null, 30, 8),
            Product("Arándanos deshidratados", deshidratados, "Arándanos deshidratados levemente endulzados.", 3290, "Bolsa 200 g", 200, "Chile", null, 15, 5),
            Product("Damascos deshidratados", deshidratados, "Huesillos/damascos secos naturales.", 2790, "Bolsa 250 g", 250, "Chile", null, 9, 5),
        ).forEach { p -> products.saveAndFlush(p); p.qrCode = "OYL-P-%04d".format(p.id) }
    }
}
