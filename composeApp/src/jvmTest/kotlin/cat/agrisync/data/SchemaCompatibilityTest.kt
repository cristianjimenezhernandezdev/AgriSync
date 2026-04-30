package cat.agrisync.data

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaCompatibilityTest {

    // ─── Detecció de taula absent a la caché d'esquema ───────────────────────

    @Test
    fun detectsMissingSchemaCacheTable() {
        val message = """{"code":"PGRST205","message":"Could not find the table 'public.granja_campanya_balance' in the schema cache"}"""

        assertTrue(
            SchemaCompatibility.isMissingSchemaCacheTable(
                message,
                SchemaCompatibility.optionalGranjaCampanyaBalanceTable
            )
        )
    }

    @Test
    fun doesNotDetectWrongTableNameAsMissingSchemaCacheTable() {
        val message = """{"code":"PGRST205","message":"Could not find the table 'public.granja_campanya_balance' in the schema cache"}"""

        assertFalse(
            SchemaCompatibility.isMissingSchemaCacheTable(message, "altra_taula")
        )
    }

    // ─── Detecció de columna absent: tipus_fertilitzant ──────────────────────

    @Test
    fun detectsMissingLegacyTipusColumn() {
        val message = """{"code":"42703","message":"column entrega_dejeccions.tipus_fertilitzant does not exist"}"""

        assertTrue(
            SchemaCompatibility.isMissingColumn(
                message,
                SchemaCompatibility.legacyEntregaTipusField,
                "entrega_dejeccions"
            )
        )
    }

    // ─── Detecció de columna absent: volum_m3 (error real reportat) ──────────

    @Test
    fun detectsMissingVolumM3Column() {
        // Error real retornat per Supabase quan la BDD no té les columnes de volum
        val message = """{"code":"42703","details":null,"hint":null,"message":"column entrega_dejeccions.volum_m3 does not exist"}"""

        assertTrue(
            SchemaCompatibility.isMissingColumn(
                message,
                SchemaCompatibility.legacyEntregaVolumField,
                "entrega_dejeccions"
            )
        )
    }

    @Test
    fun detectsMissingVolumM3ColumnWithoutTableFilter() {
        val message = """{"code":"42703","message":"column entrega_dejeccions.volum_m3 does not exist"}"""

        assertTrue(
            SchemaCompatibility.isMissingColumn(
                message,
                SchemaCompatibility.legacyEntregaVolumField
            )
        )
    }

    @Test
    fun doesNotFalselyDetectVolumM3InUnrelatedError() {
        val message = """{"code":"42703","message":"column aplicacions_fertilitzants.entrega_id does not exist"}"""

        assertFalse(
            SchemaCompatibility.isMissingColumn(
                message,
                SchemaCompatibility.legacyEntregaVolumField,
                "entrega_dejeccions"
            )
        )
    }

    @Test
    fun detectsMissingColumnReturnsFalseForWrongCode() {
        // Codi d'error diferent (no és 42703): no ha de detectar-se com columna absent
        val message = """{"code":"PGRST200","message":"column entrega_dejeccions.volum_m3 does not exist"}"""

        assertFalse(
            SchemaCompatibility.isMissingColumn(
                message,
                SchemaCompatibility.legacyEntregaVolumField
            )
        )
    }

    @Test
    fun detectsMissingColumnReturnsFalseForNullMessage() {
        assertFalse(
            SchemaCompatibility.isMissingColumn(null, SchemaCompatibility.legacyEntregaVolumField)
        )
    }

    // ─── Detecció de relació absent ──────────────────────────────────────────

    @Test
    fun detectsMissingRelationship() {
        val message = """{"code":"PGRST200","message":"Could not find a relationship between 'aplicacions_fertilitzants' and 'entrega_id' in the schema cache"}"""

        assertTrue(SchemaCompatibility.isMissingRelationship(message))
    }

    // ─── Selects segurs: no inclouen camps legacy ──────────────────────────

    @Test
    fun safeReadQueriesDoNotUseLegacyEntregaFields() {
        assertFalse(SchemaCompatibility.agricolaAplicacioSelect.contains("entrega_id"))
        assertFalse(SchemaCompatibility.danPreparationAplicacioSelect.contains("entrega_id"))
        assertFalse(SchemaCompatibility.ramaderEntregaSelect.contains("tipus_fertilitzant"))
        assertFalse(SchemaCompatibility.danPreparationEntregaSelect.contains("tipus_fertilitzant"))
    }

    @Test
    fun fallbackSelectsDoNotIncludeVolumColumns() {
        // Els selects de fallback han d'ometre les columnes de volum/nitrogen
        assertFalse(SchemaCompatibility.ramaderEntregaSelectNoVolum.contains("volum_m3"))
        assertFalse(SchemaCompatibility.ramaderEntregaSelectNoVolum.contains("kg_n_m3"))
        assertFalse(SchemaCompatibility.ramaderEntregaSelectNoVolum.contains("kg_n,"))
        assertFalse(SchemaCompatibility.danPreparationEntregaSelectNoVolum.contains("volum_m3"))
        assertFalse(SchemaCompatibility.danPreparationEntregaSelectNoVolum.contains("kg_n_m3"))
        assertFalse(SchemaCompatibility.danPreparationEntregaSelectNoVolum.contains("kg_n,"))
    }

    @Test
    fun normalSelectsDoIncludeVolumColumns() {
        // Els selects normals han d'incloure les columnes de volum/nitrogen
        assertTrue(SchemaCompatibility.ramaderEntregaSelect.contains("volum_m3"))
        assertTrue(SchemaCompatibility.ramaderEntregaSelect.contains("kg_n_m3"))
        assertTrue(SchemaCompatibility.danPreparationEntregaSelect.contains("volum_m3"))
        assertTrue(SchemaCompatibility.danPreparationEntregaSelect.contains("kg_n_m3"))
    }

    @Test
    fun fallbackSelectsStillIncludeCoreForeignKeys() {
        // Els fallbacks han de mantenir les relacions principals
        assertTrue(SchemaCompatibility.ramaderEntregaSelectNoVolum.contains("granja_origen_id"))
        assertTrue(SchemaCompatibility.ramaderEntregaSelectNoVolum.contains("terra_desti_id"))
        assertTrue(SchemaCompatibility.ramaderEntregaSelectNoVolum.contains("granja_origen:granja_origen_id"))
        assertTrue(SchemaCompatibility.danPreparationEntregaSelectNoVolum.contains("granja_origen:granja_origen_id"))
        assertTrue(SchemaCompatibility.danPreparationEntregaSelectNoVolum.contains("terra_desti:terra_desti_id"))
    }

    // ─── Serialització: no inclou camps legacy al JSON enviat ────────────────

    @Test
    fun entregaCreateRequestDoesNotSerializeLegacyTypeField() {
        val json = SupabaseJson.instance.encodeToString(
            EntregaCreateRequest(
                dan_id = "dan-1",
                granja_origen_id = "granja-1",
                data = "2026-04-30",
                terra_desti_id = "terra-1",
                volum_m3 = 10.0,
                kg_n_m3 = 3.0,
                kg_n = 30.0
            )
        )

        assertFalse(json.contains("tipus_fertilitzant"))
    }

    @Test
    fun entregaCreateRequestSerializesVolumFields() {
        val json = SupabaseJson.instance.encodeToString(
            EntregaCreateRequest(
                dan_id = "dan-1",
                granja_origen_id = "granja-1",
                data = "2026-04-30",
                terra_desti_id = "terra-1",
                volum_m3 = 50.0,
                kg_n_m3 = 4.0,
                kg_n = 200.0
            )
        )

        assertTrue(json.contains("volum_m3"))
        assertTrue(json.contains("50.0"))
        assertTrue(json.contains("kg_n_m3"))
        assertTrue(json.contains("4.0"))
        assertTrue(json.contains("\"kg_n\""))
        assertTrue(json.contains("200.0"))
    }

    @Test
    fun entregaUpdateRequestDoesNotSerializeLegacyTypeField() {
        val json = SupabaseJson.instance.encodeToString(
            EntregaUpdateRequest(
                data = "2026-04-30",
                terra_desti_id = "terra-1",
                volum_m3 = 10.0,
                kg_n_m3 = 3.0,
                kg_n = 30.0
            )
        )

        assertFalse(json.contains("tipus_fertilitzant"))
    }

    @Test
    fun aplicacioCreateRequestOmitsLegacyEntregaFieldWhenNull() {
        val json = SupabaseJson.instance.encodeToString(
            AplicacioCreateRequest(
                dan_id = "dan-1",
                terra_id = "terra-1",
                entrega_id = null,
                data = "2026-04-30",
                tipus_fertilitzant = "Mineral",
                procedencia = "Manual",
                volum_m3 = 12.0,
                kg_n_m3 = 2.5,
                kg_n = 30.0
            )
        )

        assertFalse(json.contains("entrega_id"))
    }
}
