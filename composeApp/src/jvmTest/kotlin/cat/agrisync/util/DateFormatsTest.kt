package cat.agrisync.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DateFormatsTest {

    // ─── normalizeDateInput ──────────────────────────────────────────────────

    @Test
    fun `normalizeDateInput formata 8 digits seguida`() {
        assertEquals("15/06/2024", normalizeDateInput("15062024"))
    }

    @Test
    fun `normalizeDateInput ignora caracters no numerics`() {
        assertEquals("15/06/2024", normalizeDateInput("15-06-2024"))
    }

    @Test
    fun `normalizeDateInput trunca a 8 digits si en sobren`() {
        assertEquals("15/06/2024", normalizeDateInput("150620249999"))
    }

    @Test
    fun `normalizeDateInput amb 4 digits mostra dia i mes`() {
        assertEquals("15/06", normalizeDateInput("1506"))
    }

    @Test
    fun `normalizeDateInput amb 2 digits mostra nomes dia`() {
        assertEquals("15", normalizeDateInput("15"))
    }

    @Test
    fun `normalizeDateInput buit retorna buit`() {
        assertEquals("", normalizeDateInput(""))
    }

    // ─── parseEnteredDateToIso ───────────────────────────────────────────────

    @Test
    fun `parseEnteredDateToIso converteix dd-MM-yyyy a ISO`() {
        assertEquals("2024-06-15", parseEnteredDateToIso("15/06/2024"))
    }

    @Test
    fun `parseEnteredDateToIso accepta ISO directament`() {
        assertEquals("2024-06-15", parseEnteredDateToIso("2024-06-15"))
    }

    @Test
    fun `parseEnteredDateToIso retorna null si buit`() {
        assertNull(parseEnteredDateToIso(""))
        assertNull(parseEnteredDateToIso("   "))
    }

    @Test
    fun `parseEnteredDateToIso retorna null amb dia 00`() {
        assertNull(parseEnteredDateToIso("00/06/2024"))
    }

    @Test
    fun `parseEnteredDateToIso retorna null amb mes 13`() {
        assertNull(parseEnteredDateToIso("01/13/2024"))
    }

    @Test
    fun `parseEnteredDateToIso retorna null amb text aleatori`() {
        assertNull(parseEnteredDateToIso("no es una data"))
    }

    // ─── any de traspas ──────────────────────────────────────────────────────

    @Test
    fun `accepta 29 de febrer en any de traspas`() {
        assertEquals("2024-02-29", parseEnteredDateToIso("29/02/2024"))
    }

    @Test
    fun `rebutja 29 de febrer en any no de traspas`() {
        assertNull(parseEnteredDateToIso("29/02/2023"))
    }

    @Test
    fun `accepta 29 de febrer en any divisible per 400`() {
        assertEquals("2000-02-29", parseEnteredDateToIso("29/02/2000"))
    }

    @Test
    fun `rebutja 29 de febrer en any divisible per 100 pero no per 400`() {
        assertNull(parseEnteredDateToIso("29/02/1900"))
    }

    // ─── formatStoredDateForDisplay ──────────────────────────────────────────

    @Test
    fun `formatStoredDateForDisplay converteix ISO a format visual`() {
        assertEquals("15/06/2024", formatStoredDateForDisplay("2024-06-15"))
    }

    @Test
    fun `formatStoredDateForDisplay retorna guio si null`() {
        assertEquals("-", formatStoredDateForDisplay(null))
    }

    @Test
    fun `formatStoredDateForDisplay retorna guio si buit`() {
        assertEquals("-", formatStoredDateForDisplay(""))
    }

    // ─── formatStoredDateForInput ────────────────────────────────────────────

    @Test
    fun `formatStoredDateForInput converteix ISO per al camp d entrada`() {
        assertEquals("15/06/2024", formatStoredDateForInput("2024-06-15"))
    }

    @Test
    fun `formatStoredDateForInput retorna buit si null`() {
        assertEquals("", formatStoredDateForInput(null))
    }

    @Test
    fun `formatStoredDateForInput retorna buit si buit`() {
        assertEquals("", formatStoredDateForInput(""))
    }

    // ─── round-trip ──────────────────────────────────────────────────────────

    @Test
    fun `round-trip display a ISO i de tornada a display`() {
        val display = "31/12/2025"
        val iso = parseEnteredDateToIso(display)
        assertNotNull(iso)
        assertEquals(display, formatStoredDateForDisplay(iso))
    }

    @Test
    fun `round-trip ISO a display i de tornada a ISO`() {
        val iso = "2024-03-01"
        val display = formatStoredDateForDisplay(iso)
        assertEquals(iso, parseEnteredDateToIso(display))
    }
}

