package cl.oyl.frutossecos.data.local.seed

import cl.oyl.frutossecos.data.local.entity.CategoryEntity
import cl.oyl.frutossecos.data.local.entity.ProductEntity
import cl.oyl.frutossecos.data.local.entity.StoreEntity
import cl.oyl.frutossecos.domain.model.StoreType

/** Datos ficticios de demostración. */
object SeedData {
    // ---- Administrador demo (cuenta "en duro") ----
    const val ADMIN_NAME = "Administrador"
    const val ADMIN_ALIAS = "admin"
    const val ADMIN_EMAIL = "admin@admin.cl"
    const val ADMIN_PHONE = "+56 9 1234 1234"
    const val ADMIN_ADDRESS = "Santiago"
    /** Contraseña de demo (documentada en el README). Solo se guarda su hash PBKDF2 + sal. */
    const val DEMO_ADMIN_PASSWORD = "OylAdmin2026!"

    const val CATEGORY_FRUTOS_SECOS = 1L
    const val CATEGORY_SEMILLAS = 2L
    const val CATEGORY_DESHIDRATADOS = 3L

    /** "Todos" es un filtro de la UI (no una fila), por eso no se siembra. */
    val categories = listOf(
        CategoryEntity(CATEGORY_FRUTOS_SECOS, "Frutos secos"),
        CategoryEntity(CATEGORY_SEMILLAS, "Semillas"),
        CategoryEntity(CATEGORY_DESHIDRATADOS, "Deshidratados"),
    )

    val stores = listOf(
        StoreEntity(1, "OYL Frutos Secos – Tienda Providencia", "Av. Providencia 1234, Providencia, Santiago", -33.4263, -70.6170, "Lun a Sáb 09:00–20:00", StoreType.TIENDA),
        StoreEntity(2, "Punto de retiro Ñuñoa", "Irarrázaval 3456, Ñuñoa, Santiago", -33.4545, -70.5980, "Lun a Vie 10:00–19:00", StoreType.RETIRO),
        StoreEntity(3, "Punto de retiro Santiago Centro", "Huérfanos 789, Santiago Centro", -33.4389, -70.6510, "Lun a Vie 09:30–18:30", StoreType.RETIRO),
    )

    fun products(now: Long): List<ProductEntity> = listOf(
        p(1, "Almendras", CATEGORY_FRUTOS_SECOS, "Almendras enteras naturales, sin sal.", 2990, "Bolsa 250 g", 250, "Chile", 25, 5, now),
        p(2, "Nueces", CATEGORY_FRUTOS_SECOS, "Nueces mariposa extra light.", 3490, "Bolsa 250 g", 250, "Chile", 18, 5, now),
        p(3, "Mix Premium", CATEGORY_FRUTOS_SECOS, "Mezcla de almendras, nueces, castañas de cajú y pasas.", 3990, "Bolsa 250 g", 250, "Chile / Brasil", 12, 5, now),
        p(4, "Maní tostado", CATEGORY_FRUTOS_SECOS, "Maní tostado sin sal.", 1490, "Bolsa 250 g", 250, "Argentina", 40, 10, now),
        p(5, "Castañas de cajú", CATEGORY_FRUTOS_SECOS, "Castañas de cajú enteras.", 4490, "Bolsa 250 g", 250, "Brasil", 4, 5, now),
        p(6, "Semillas de chía", CATEGORY_SEMILLAS, "Chía orgánica, fuente de omega 3.", 1990, "Bolsa 200 g", 200, "Bolivia", 30, 8, now),
        p(7, "Semillas de maravilla", CATEGORY_SEMILLAS, "Maravilla pelada tostada.", 1590, "Bolsa 250 g", 250, "Chile", 22, 8, now),
        p(8, "Linaza", CATEGORY_SEMILLAS, "Linaza dorada entera.", 1290, "Bolsa 250 g", 250, "Chile", 0, 5, now),
        p(9, "Pasas", CATEGORY_DESHIDRATADOS, "Pasas morenas sin semilla.", 1790, "Bolsa 250 g", 250, "Chile", 30, 8, now),
        p(10, "Arándanos deshidratados", CATEGORY_DESHIDRATADOS, "Arándanos deshidratados levemente endulzados.", 3290, "Bolsa 200 g", 200, "Chile", 15, 5, now),
        p(11, "Damascos deshidratados", CATEGORY_DESHIDRATADOS, "Huesillos/damascos secos naturales.", 2790, "Bolsa 250 g", 250, "Chile", 9, 5, now),
    )

    /** Formato del QR de producto: OYL-P-0001. */
    fun qrFor(productId: Long): String = "OYL-P-%04d".format(productId)

    private fun p(
        id: Long, name: String, categoryId: Long, description: String, price: Long, presentation: String,
        grams: Int, origin: String, stock: Int, minStock: Int, now: Long,
    ) = ProductEntity(
        id = id, name = name, categoryId = categoryId, description = description, price = price,
        presentation = presentation, weightGrams = grams, origin = origin, imageUrl = null, stock = stock,
        minStock = minStock, active = true, qrCode = qrFor(id), updatedAt = now,
    )
}
