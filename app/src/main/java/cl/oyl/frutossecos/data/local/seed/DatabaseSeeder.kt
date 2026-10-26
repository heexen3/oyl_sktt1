package cl.oyl.frutossecos.data.local.seed

import androidx.room.withTransaction
import cl.oyl.frutossecos.core.util.PasswordHasher
import cl.oyl.frutossecos.data.local.OylDatabase
import cl.oyl.frutossecos.data.local.entity.UserEntity
import cl.oyl.frutossecos.domain.model.Role
import javax.inject.Inject
import javax.inject.Singleton

/** Siembra idempotente: solo inserta lo que falta (primer arranque). */
@Singleton
class DatabaseSeeder @Inject constructor(private val db: OylDatabase) {

    suspend fun seedIfNeeded(now: Long = System.currentTimeMillis()) {
        db.withTransaction {
            if (db.categoryDao().count() == 0) db.categoryDao().upsertAll(SeedData.categories)
            if (db.storeDao().count() == 0) db.storeDao().upsertAll(SeedData.stores)
            if (db.productDao().count() == 0) db.productDao().upsertAll(SeedData.products(now))
            if (db.userDao().findByEmail(SeedData.ADMIN_EMAIL) == null) {
                val hashed = PasswordHasher.hash(SeedData.DEMO_ADMIN_PASSWORD)
                db.userDao().insert(
                    UserEntity(
                        name = SeedData.ADMIN_NAME,
                        alias = SeedData.ADMIN_ALIAS,
                        email = SeedData.ADMIN_EMAIL,
                        phone = SeedData.ADMIN_PHONE,
                        address = SeedData.ADMIN_ADDRESS,
                        passwordHash = hashed.hash,
                        salt = hashed.salt,
                        role = Role.ADMINISTRADOR,
                    )
                )
            }
        }
    }
}
