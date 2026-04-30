package cat.agrisync.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NitrogenMathTest {

    // ─── parseDecimalInput ───────────────────────────────────────────────────

    @Test
    fun `parseDecimalInput accepta nombre enter`() {
        assertEquals(100.0, parseDecimalInput("100"))
    }

    @Test
    fun `parseDecimalInput accepta decimal amb punt`() {
        assertEquals(3.5, parseDecimalInput("3.5"))
    }

    @Test
    fun `parseDecimalInput accepta coma i converteix a punt`() {
        assertEquals(3.5, parseDecimalInput("3,5"))
    }

    @Test
    fun `parseDecimalInput ignora espais laterals`() {
        assertEquals(10.0, parseDecimalInput("  10  "))
    }

    @Test
    fun `parseDecimalInput retorna null si buit`() {
        assertNull(parseDecimalInput(""))
        assertNull(parseDecimalInput("   "))
    }

    @Test
    fun `parseDecimalInput retorna null si text no numeric`() {
        assertNull(parseDecimalInput("abc"))
        assertNull(parseDecimalInput("12.3.4"))
    }

    // ─── validateAndResolveNitrogenTriplet ───────────────────────────────────

    @Test
    fun `resol kgN donat volum i concentracio`() {
        // 50 m3 x 4 kgN per m3 = 200 kgN
        val result = validateAndResolveNitrogenTriplet("", "50", "4")
        assertNull(result.errorMessage)
        assertNotNull(result.values)
        assertEquals(200.0, result.values.kgN)
        assertEquals(50.0,  result.values.volumM3)
        assertEquals(4.0,   result.values.kgNPerM3)
    }

    @Test
    fun `resol volum donat kgN i concentracio`() {
        // 200 kgN / 4 kgN per m3 = 50 m3
        val result = validateAndResolveNitrogenTriplet("200", "", "4")
        assertNull(result.errorMessage)
        assertEquals(50.0, result.values!!.volumM3)
    }

    @Test
    fun `resol concentracio donat kgN i volum`() {
        // 200 kgN / 50 m3 = 4 kgN per m3
        val result = validateAndResolveNitrogenTriplet("200", "50", "")
        assertNull(result.errorMessage)
        assertEquals(4.0, result.values!!.kgNPerM3)
    }

    @Test
    fun `accepta tres camps consistents`() {
        val result = validateAndResolveNitrogenTriplet("200", "50", "4")
        assertNull(result.errorMessage)
        assertNotNull(result.values)
    }

    @Test
    fun `rebutja tres camps inconsistents`() {
        // 200 != 50 x 4.1 (diferencia de 5 kgN)
        val result = validateAndResolveNitrogenTriplet("200", "50", "4.1")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("igual"))
    }

    @Test
    fun `error si nomes un camp informat`() {
        val result = validateAndResolveNitrogenTriplet("200", "", "")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("almenys 2"))
    }

    @Test
    fun `error si cap camp informat`() {
        val result = validateAndResolveNitrogenTriplet("", "", "")
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `error si volum es zero i cal calcular concentracio`() {
        val result = validateAndResolveNitrogenTriplet("100", "0", "")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("superior a 0"))
    }

    @Test
    fun `error si concentracio es zero i cal calcular volum`() {
        val result = validateAndResolveNitrogenTriplet("100", "", "0")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("superior a 0"))
    }

    @Test
    fun `error si kgN es negatiu`() {
        val result = validateAndResolveNitrogenTriplet("-100", "50", "")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("negatius"))
    }

    @Test
    fun `error si volum es negatiu`() {
        val result = validateAndResolveNitrogenTriplet("100", "-50", "")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("negatius"))
    }

    @Test
    fun `error si kgN no es numeric`() {
        val result = validateAndResolveNitrogenTriplet("abc", "50", "4")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("Kg N"))
    }

    @Test
    fun `error si volum no es numeric`() {
        val result = validateAndResolveNitrogenTriplet("200", "xx", "4")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("volum"))
    }

    @Test
    fun `error si concentracio no es numerica`() {
        val result = validateAndResolveNitrogenTriplet("200", "50", "?")
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage.contains("kg N/m3"))
    }

    // ─── autofillNitrogenTexts ───────────────────────────────────────────────

    @Test
    fun `autofill omple kgN quan es dona volum i concentracio`() {
        val base = NitrogenTripletTexts(kgN = "", volumM3 = "50", kgNPerM3 = "4")
        val result = autofillNitrogenTexts(base, NitrogenField.RATE_KG_N_M3, "4")
        assertEquals("200", result.kgN)
    }

    @Test
    fun `autofill omple volum quan es dona kgN i concentracio`() {
        val base = NitrogenTripletTexts(kgN = "200", volumM3 = "", kgNPerM3 = "")
        val result = autofillNitrogenTexts(base, NitrogenField.RATE_KG_N_M3, "4")
        assertEquals("50", result.volumM3)
    }

    @Test
    fun `autofill omple concentracio quan es dona kgN i volum`() {
        val base = NitrogenTripletTexts(kgN = "200", volumM3 = "50", kgNPerM3 = "")
        val result = autofillNitrogenTexts(base, NitrogenField.VOLUME_M3, "50")
        assertEquals("4", result.kgNPerM3)
    }

    @Test
    fun `autofill no modifica res amb un sol camp informat`() {
        val base = NitrogenTripletTexts(kgN = "", volumM3 = "", kgNPerM3 = "")
        val result = autofillNitrogenTexts(base, NitrogenField.TOTAL_KG_N, "200")
        assertEquals("", result.volumM3)
        assertEquals("", result.kgNPerM3)
    }
}



