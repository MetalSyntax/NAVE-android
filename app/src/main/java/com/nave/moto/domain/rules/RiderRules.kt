package com.nave.moto.domain.rules

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/** Tamaños de texto: Pequeña, Normal (por defecto), Grande, Muy grande, Máxima. */
val FONT_SCALES = listOf(0.9f, 1f, 1.15f, 1.3f, 1.5f)

/** Ajusta cualquier valor guardado (versiones anteriores, datos raros) a la opción más cercana; sin valor → Normal. */
fun normalizeFontScale(v: Float?): Float =
    if (v == null || v.isNaN()) 1f else FONT_SCALES.minByOrNull { abs(it - v) } ?: 1f

val BLOOD_TYPES = listOf("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-")

/** Solo dígitos y '+': lo que el marcador del teléfono necesita. */
fun dialable(phone: String): String = phone.filter { it.isDigit() || it == '+' }

/** Días hasta [date] (negativo = vencido); null si no hay fecha válida. */
fun daysUntil(date: String, today: LocalDate = LocalDate.now()): Long? =
    runCatching { ChronoUnit.DAYS.between(today, LocalDate.parse(date)) }.getOrNull()

/** Entradas del .zip de respaldo que se pueden restaurar (evita rutas ajenas). */
fun isRestorableEntry(name: String): Boolean =
    listOf("files/photos/", "files/manuals/", "files/tracks/").any { name.startsWith(it) } && !name.contains("..")

/** Rutas relativas que viajan en el respaldo: fotos, manuales, foto de perfil y un CSV GPS por ruta. */
fun backupRelPaths(photos: List<String?>, manuals: List<String>, routeIds: List<Long>, riderPhoto: String?): List<String> =
    (photos.filterNotNull() + manuals + listOfNotNull(riderPhoto) + routeIds.map { "tracks/route-$it.csv" }).distinct()
