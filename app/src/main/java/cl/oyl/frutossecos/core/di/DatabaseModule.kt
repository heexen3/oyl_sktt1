package cl.oyl.frutossecos.core.di

import android.content.Context
import androidx.room.Room
import cl.oyl.frutossecos.data.local.OylDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OylDatabase =
        Room.databaseBuilder(context, OylDatabase::class.java, OylDatabase.NAME).build()

    @Provides fun userDao(db: OylDatabase) = db.userDao()
    @Provides fun sessionDao(db: OylDatabase) = db.sessionDao()
    @Provides fun categoryDao(db: OylDatabase) = db.categoryDao()
    @Provides fun productDao(db: OylDatabase) = db.productDao()
    @Provides fun cartDao(db: OylDatabase) = db.cartDao()
    @Provides fun orderDao(db: OylDatabase) = db.orderDao()
    @Provides fun notificationDao(db: OylDatabase) = db.notificationDao()
    @Provides fun storeDao(db: OylDatabase) = db.storeDao()
    @Provides fun pendingStockMovementDao(db: OylDatabase) = db.pendingStockMovementDao()
}
