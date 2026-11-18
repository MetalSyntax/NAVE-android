package com.nave.moto

import com.nave.moto.domain.rules.*
import com.nave.moto.domain.tracking.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TrackingAndDatesTest {
    // 0.01 grados de latitud ≈ 1111 m
    private fun p(latOff: Double, t: Long, acc: Float = 5f) = TrackPoint(10.5 + latOff, -66.9, t, acc)

    @Test fun haversineOneHundredthDegree() {
        assertEquals(1111.9, haversineMeters(p(0.0, 0), p(0.01, 0)), 2.0)
    }

    @Test fun accumulatesRoute() {
        val a = TrackAccumulator()
        assertTrue(a.add(p(0.0, 0)))
        assertTrue(a.add(p(0.01, 60_000)))   // ~1,1 km en 60 s (~67 km/h)
        assertTrue(a.add(p(0.02, 120_000)))
        assertEquals(2224.0, a.distanceM, 5.0)
        assertEquals(3, a.accepted)
    }

    @Test fun ignoresNoiseAndBadFixes() {
        val a = TrackAccumulator()
        a.add(p(0.0, 0))
        assertFalse(a.add(p(0.00002, 3_000)))          // ~2 m: jitter parado
        assertFalse(a.add(p(0.01, 6_000, acc = 120f))) // impreciso
        assertFalse(a.add(p(0.5, 9_000)))              // salto imposible
        assertEquals(0.0, a.distanceM, 0.0)
        assertEquals(1, a.accepted)
    }

    @Test fun quickDatesPastAndExpiry() {
        val today = LocalDate.of(2026, 3, 1)
        val past = quickDates(today)
        assertEquals(listOf(today, LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 27)), past.map { it.date })
        assertEquals(QuickKey.TODAY, past.first().key)
        val exp = quickDates(LocalDate.of(2026, 8, 31), expiry = true)
        assertEquals(LocalDate.of(2027, 2, 28), exp[0].date) // +6 meses recorta fin de mes
        assertEquals(LocalDate.of(2027, 8, 31), exp[1].date)
    }

    @Test fun pickerMillisRoundTrip() {
        val d = LocalDate.of(2026, 11, 17)
        assertEquals(d, pickerMillisToDate(d.toPickerMillis()))
    }
}
