package cl.oyl.frutossecos

import android.app.Application
import cl.oyl.frutossecos.core.di.ApplicationScope
import cl.oyl.frutossecos.data.local.seed.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration

@HiltAndroidApp
class OylApplication : Application() {
    @Inject lateinit var seeder: DatabaseSeeder
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // osmdroid exige un User-Agent propio para usar los tiles de OpenStreetMap.
        Configuration.getInstance().userAgentValue = packageName
        // Siembra el admin demo, categorías, tiendas y productos en el primer arranque.
        appScope.launch { seeder.seedIfNeeded() }
    }
}
