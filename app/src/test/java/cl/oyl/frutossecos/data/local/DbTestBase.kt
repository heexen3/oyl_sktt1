package cl.oyl.frutossecos.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cl.oyl.frutossecos.data.local.entity.UserEntity
import cl.oyl.frutossecos.data.local.seed.DatabaseSeeder
import cl.oyl.frutossecos.domain.model.Role
import org.junit.After
import org.junit.Before

/** Base para pruebas con Room en memoria (Robolectric). */
abstract class DbTestBase {
    protected lateinit var db: OylDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), OylDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    protected suspend fun seed() = DatabaseSeeder(db).seedIfNeeded(now = 1_000L)

    protected suspend fun newClient(alias: String): Long = db.userDao().insert(
        UserEntity(name = alias, alias = alias, email = "$alias@correo.cl", phone = "+56 9 0000 0000",
            passwordHash = "h", salt = "s", role = Role.CLIENTE)
    )
}
