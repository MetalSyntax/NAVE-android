package com.nave.moto

import com.nave.moto.domain.rules.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RiderRulesTest {
    @Test fun fontScaleMigratesToNearestOption() {
        assertEquals(1f, normalizeFontScale(null))
        assertEquals(1f, normalizeFontScale(1f))
        assertEquals(0.9f, normalizeFontScale(0.8f))
        assertEquals(1.15f, normalizeFontScale(1.15f))
        assertEquals(1.5f, normalizeFontScale(3f))
        assertEquals(1f, normalizeFontScale(Float.NaN))
    }

    @Test fun dialableKeepsDigitsAndPlus() = assertEquals("+584141234567", dialable("+58 (414) 123-4567"))

    @Test fun licenseDays() {
        val t = LocalDate.of(2026, 11, 17)
        assertEquals(13L, daysUntil("2026-11-30", t))
        assertEquals(-1L, daysUntil("2026-11-16", t))
        assertNull(daysUntil("", t))
    }

    @Test fun backupIncludesTracksPhotosAndProfile() {
        val rels = backupRelPaths(listOf("photos/a.jpg", null), listOf("manuals/m.pdf"), listOf(3, 7), "photos/me.jpg")
        assertEquals(listOf("photos/a.jpg", "manuals/m.pdf", "photos/me.jpg", "tracks/route-3.csv", "tracks/route-7.csv"), rels)
    }

    @Test fun restoreAcceptsTracksButNotTraversal() {
        assertTrue(isRestorableEntry("files/tracks/route-3.csv"))
        assertFalse(isRestorableEntry("files/tracks/../../x"))
        assertFalse(isRestorableEntry("data.json"))
    }
}
