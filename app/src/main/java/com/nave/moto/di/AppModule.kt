package com.nave.moto.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.nave.moto.data.local.CatalogDao
import com.nave.moto.data.local.CatalogDatabase
import com.nave.moto.data.local.MotoDatabase
import com.nave.moto.data.local.dao.FuelLogDao
import com.nave.moto.data.local.dao.MaintenanceDao
import com.nave.moto.data.local.dao.MotoManualDao
import com.nave.moto.data.local.dao.MotorcycleDao
import com.nave.moto.data.local.dao.RouteDao
import com.nave.moto.data.local.dao.ServiceScheduleDao
import com.nave.moto.data.repository.MotoRepositoryImpl
import com.nave.moto.domain.repository.MotoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nave_moto_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMotoDatabase(@ApplicationContext context: Context): MotoDatabase =
        Room.databaseBuilder(
            context,
            MotoDatabase::class.java,
            "nave_moto.db"
        ).build()

    @Provides
    @Singleton
    fun provideCatalogDatabase(@ApplicationContext context: Context): CatalogDatabase =
        Room.databaseBuilder(context, CatalogDatabase::class.java, "catalog_v1.db")
            .createFromAsset("catalog.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideCatalogDao(db: CatalogDatabase): CatalogDao = db.catalogDao()

    @Provides
    fun provideMotorcycleDao(db: MotoDatabase): MotorcycleDao = db.motorcycleDao()

    @Provides
    fun provideFuelLogDao(db: MotoDatabase): FuelLogDao = db.fuelLogDao()

    @Provides
    fun provideMaintenanceDao(db: MotoDatabase): MaintenanceDao = db.maintenanceDao()

    @Provides
    fun provideServiceScheduleDao(db: MotoDatabase): ServiceScheduleDao = db.serviceScheduleDao()

    @Provides
    fun provideRouteDao(db: MotoDatabase): RouteDao = db.routeDao()

    @Provides
    fun provideMotoManualDao(db: MotoDatabase): MotoManualDao = db.motoManualDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMotoRepository(impl: MotoRepositoryImpl): MotoRepository

    @Binds
    @Singleton
    abstract fun bindBackup(impl: com.nave.moto.data.backup.BackupServiceImpl): com.nave.moto.domain.repository.BackupService
}
