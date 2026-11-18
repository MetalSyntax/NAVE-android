package com.nave.moto.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_models", indices = [Index("brand")])
data class CatalogModelEntity(
    @PrimaryKey val id: Long,
    val brand: String,
    val name: String,
    val category: String,
    val cc: Int,
    val tankL: Double,
    val kmL: Double,
    val maxSpeedKmh: Int,
    val weightKg: Int,
    val colors: String,
    val source: String,
    val hasThumb: Int
)

@Entity(tableName = "catalog_specs", indices = [Index("modelId")])
data class CatalogSpecEntity(
    @PrimaryKey val id: Long,
    val modelId: Long,
    val section: String,
    val body: String
)

@Entity(tableName = "catalog_thumbs")
data class CatalogThumbEntity(
    @PrimaryKey val modelId: Long,
    val data: ByteArray
)
