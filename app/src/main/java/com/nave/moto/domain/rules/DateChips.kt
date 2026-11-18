package com.nave.moto.domain.rules

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

enum class QuickKey { TODAY, YESTERDAY, TWO_DAYS, SIX_MONTHS, ONE_YEAR, TWO_YEARS }

data class QuickDate(val key: QuickKey, val date: LocalDate)

/** Atajos de fecha: pasadas (registros) o de vencimiento (póliza). */
fun quickDates(today: LocalDate, expiry: Boolean = false): List<QuickDate> =
    if (expiry) listOf(
        QuickDate(QuickKey.SIX_MONTHS, today.plusMonths(6)),
        QuickDate(QuickKey.ONE_YEAR, today.plusYears(1)),
        QuickDate(QuickKey.TWO_YEARS, today.plusYears(2))
    ) else listOf(
        QuickDate(QuickKey.TODAY, today),
        QuickDate(QuickKey.YESTERDAY, today.minusDays(1)),
        QuickDate(QuickKey.TWO_DAYS, today.minusDays(2))
    )

/** El DatePicker de Material3 trabaja en milisegundos UTC a medianoche. */
fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun pickerMillisToDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
