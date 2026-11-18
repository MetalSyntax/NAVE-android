package com.nave.moto.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import com.nave.moto.data.local.entity.CatalogModelEntity
import com.nave.moto.data.local.entity.CatalogSpecEntity

@Dao
interface CatalogDao {
    @Query("SELECT DISTINCT brand FROM catalog_models ORDER BY brand")
    suspend fun brands(): List<String>

    @Query("SELECT * FROM catalog_models WHERE brand = :brand ORDER BY name")
    suspend fun modelsOf(brand: String): List<CatalogModelEntity>

    @Query(
        "SELECT * FROM catalog_models WHERE name LIKE '%' || :q || '%' OR brand LIKE '%' || :q || '%' " +
            "ORDER BY brand, name LIMIT 80"
    )
    suspend fun search(q: String): List<CatalogModelEntity>

    @Query("SELECT * FROM catalog_models WHERE id = :id")
    suspend fun model(id: Long): CatalogModelEntity?

    @Query("SELECT * FROM catalog_specs WHERE modelId = :id ORDER BY id")
    suspend fun specs(id: Long): List<CatalogSpecEntity>

    @Query("SELECT data FROM catalog_thumbs WHERE modelId = :id")
    suspend fun thumb(id: Long): ByteArray?
}

/** BD empaquetada de solo lectura (assets/catalog.db). Generada desde Apex-Velocity/motos.db. */
@Database(
    entities = [
        CatalogModelEntity::class,
        CatalogSpecEntity::class,
        com.nave.moto.data.local.entity.CatalogThumbEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
}
