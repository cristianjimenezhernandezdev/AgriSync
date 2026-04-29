package cat.agrisync.data

import kotlinx.serialization.Serializable

@Serializable
data class TitularAccessRow(
    val titular_id: String,
    val nom: String,
    val nif: String? = null,
    val telefon: String? = null,
    val email: String? = null,
    val adreca: String? = null,
    val codi_postal: String? = null,
    val can_agricola: Boolean = false,
    val can_ramader: Boolean = false,
    val last_update_at: String? = null,
    val last_update_by: String? = null
)

@Serializable
data class TerraDto(
    val id: String,
    val titular_id: String? = null,
    val codi_sigpac_complet: String? = null,
    val municipi_literal: String? = null,
    val us_sigpac: String? = null,
    val cultiu: String? = null,
    val superficie: Double? = null,
    val zona: String = "ZNV",
    val limit_kg_n_ha: Double? = null,
    val updated_at: String? = null,
    val updated_by: String? = null,
    val titular: TitularDto? = null
)

@Serializable
data class DanRefDto(
    val id: String,
    val titular_id: String? = null,
    val campanya: Int? = null
)

@Serializable
data class AplicacioFertilitzantDto(
    val id: String,
    val terra_id: String? = null,
    val entrega_id: String? = null,
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null,
    val tecnic_id: String? = null,
    val updated_at: String? = null,
    val updated_by: String? = null,
    val entrega: EntregaAplicacioLinkDto? = null,
    val dan: DanRefDto? = null
)

@Serializable
data class EntregaAplicacioLinkDto(
    val id: String,
    val granja_origen: GranjaDto? = null
)

@Serializable
data class GranjaDto(
    val id: String,
    val titular_id: String,
    val marca_oficial: String,
    val nom: String? = null,
    val updated_at: String? = null,
    val updated_by: String? = null
)

@Serializable
data class BestiarDto(
    val id: String,
    val codi: String,
    val descripcio: String? = null
)

@Serializable
data class FaseProductivaDto(
    val id: String,
    val codi: String,
    val descripcio: String? = null
)

@Serializable
data class GranjaBestiarDto(
    val id: String,
    val cens: Double? = null,
    val updated_at: String? = null,
    val updated_by: String? = null,
    val granja: GranjaDto? = null,
    val bestiar: BestiarDto? = null,
    val fase_productiva: FaseProductivaDto? = null
)

@Serializable
data class EntregaDejeccioDto(
    val id: String,
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null,
    val granja_origen_id: String,
    val terra_desti_id: String? = null,
    val updated_at: String? = null,
    val updated_by: String? = null,
    val dan: DanRefDto? = null,
    val granja_origen: GranjaDto? = null,
    val terra_desti: TerraDto? = null,
    val aplicacio_generada: AplicacioFertilitzantDto? = null
)

@Serializable
data class GranjaCampanyaBalanceDto(
    val id: String,
    val granja_id: String,
    val dan: DanRefDto? = null,
    val granja: GranjaDto? = null,
    val estoc_inicial_kg_n: Double? = null,
    val kg_n_generat: Double? = null,
    val estoc_final_declarat_kg_n: Double? = null,
    val updated_at: String? = null,
    val updated_by: String? = null
)

private const val RAMADER_PROCEDENCIA_PREFIX = "Entrega ramadera des de "

internal fun AplicacioFertilitzantDto.isSynchronizedFromRamader(): Boolean {
    return !entrega_id.isNullOrBlank() || entrega != null || procedencia.hasRamaderProcedencia()
}

internal fun AplicacioFertilitzantDto.ramaderOriginLabel(): String? {
    return entrega?.granja_origen?.nom
        ?: entrega?.granja_origen?.marca_oficial
        ?: procedencia.extractRamaderOriginLabel()
}

internal fun DanPreparationAplicacioDto.ramaderOriginLabel(): String? {
    return entrega?.granja_origen?.nom
        ?: entrega?.granja_origen?.marca_oficial
        ?: procedencia.extractRamaderOriginLabel()
}

private fun String?.hasRamaderProcedencia(): Boolean {
    return this?.startsWith(RAMADER_PROCEDENCIA_PREFIX) == true
}

private fun String?.extractRamaderOriginLabel(): String? {
    return this
        ?.takeIf { it.startsWith(RAMADER_PROCEDENCIA_PREFIX) }
        ?.removePrefix(RAMADER_PROCEDENCIA_PREFIX)
        ?.trim()
        ?.ifBlank { null }
}

data class TitularCollaboratingTecnicSummary(
    val id: String,
    val nom: String,
    val oficinaNom: String,
    val email: String? = null,
    val telefon: String? = null,
    val rol: String? = null,
    val scopes: List<String> = emptyList()
)

data class TitularCollaboratingOficinaSummary(
    val id: String,
    val nom: String,
    val scopes: List<String> = emptyList(),
    val hasDirectTecnics: Boolean = false,
    val hasSharedAccess: Boolean = false
)

internal fun List<TitularCollaboratingTecnicDto>.toTitularCollaboratingTecnicSummaries(): List<TitularCollaboratingTecnicSummary> {
    return this
        .mapNotNull { row ->
            val tecnic = row.tecnic ?: return@mapNotNull null
            TitularCollaboratingTecnicSummary(
                id = tecnic.id,
                nom = tecnic.nom,
                oficinaNom = tecnic.oficina?.nom ?: tecnic.oficina_id,
                email = tecnic.email,
                telefon = tecnic.telefon,
                rol = tecnic.rol,
                scopes = listOf(row.scope)
            )
        }
        .groupBy { it.id }
        .values
        .map { rows ->
            val first = rows.first()
            first.copy(scopes = sortTitularScopes(rows.flatMap { it.scopes }))
        }
        .sortedWith(compareBy({ it.oficinaNom.lowercase() }, { it.nom.lowercase() }))
}

internal fun buildTitularCollaboratingOficinaSummaries(
    tecnics: List<TitularCollaboratingTecnicDto>,
    sharedOficines: List<TitularSharedOfficeDto>
): List<TitularCollaboratingOficinaSummary> {
    val byOffice = linkedMapOf<String, TitularCollaboratingOficinaSummary>()

    tecnics.forEach { row ->
        val tecnic = row.tecnic ?: return@forEach
        val officeId = tecnic.oficina?.id ?: tecnic.oficina_id
        val current = byOffice[officeId]
        byOffice[officeId] = TitularCollaboratingOficinaSummary(
            id = officeId,
            nom = tecnic.oficina?.nom ?: tecnic.oficina_id,
            scopes = sortTitularScopes((current?.scopes ?: emptyList()) + row.scope),
            hasDirectTecnics = true,
            hasSharedAccess = current?.hasSharedAccess == true
        )
    }

    sharedOficines.forEach { row ->
        val office = row.oficina ?: return@forEach
        val current = byOffice[office.id]
        byOffice[office.id] = TitularCollaboratingOficinaSummary(
            id = office.id,
            nom = office.nom,
            scopes = sortTitularScopes((current?.scopes ?: emptyList()) + row.scope),
            hasDirectTecnics = current?.hasDirectTecnics == true,
            hasSharedAccess = true
        )
    }

    return byOffice.values.sortedBy { it.nom.lowercase() }
}

private fun sortTitularScopes(scopes: Collection<String>): List<String> {
    val order = listOf("comu", "agricola", "ramader", "lectura")
    return scopes
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .sortedWith(compareBy({ scope -> order.indexOf(scope).takeIf { it >= 0 } ?: Int.MAX_VALUE }, { it }))
}

@Serializable
data class TerraUpdateRequest(
    val municipi_literal: String? = null,
    val us_sigpac: String? = null,
    val cultiu: String? = null,
    val superficie: Double? = null,
    val zona: String? = null
)

@Serializable
data class DanCreateRequest(
    val titular_id: String,
    val campanya: Int,
    val estat: String = "en_curs"
)

@Serializable
data class AplicacioCreateRequest(
    val dan_id: String,
    val terra_id: String,
    val entrega_id: String? = null,
    val data: String,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double
)

@Serializable
data class AplicacioUpdateRequest(
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null
)

@Serializable
data class GranjaCreateRequest(
    val titular_id: String,
    val marca_oficial: String,
    val nom: String? = null
)

@Serializable
data class GranjaUpdateRequest(
    val nom: String? = null,
    val marca_oficial: String? = null
)

@Serializable
data class GranjaBestiarCreateRequest(
    val granja_id: String,
    val bestiar_id: String,
    val fase_productiva_id: String,
    val cens: Double
)

@Serializable
data class GranjaBestiarUpdateRequest(
    val cens: Double? = null
)

@Serializable
data class EntregaCreateRequest(
    val dan_id: String,
    val granja_origen_id: String,
    val data: String,
    val terra_desti_id: String,
    val tipus_fertilitzant: String? = null,
    val volum_m3: Double,
    val kg_n_m3: Double,
    val kg_n: Double
)

@Serializable
data class EntregaUpdateRequest(
    val data: String? = null,
    val terra_desti_id: String? = null,
    val tipus_fertilitzant: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null
)

@Serializable
data class GranjaCampanyaBalanceCreateRequest(
    val dan_id: String,
    val granja_id: String,
    val estoc_inicial_kg_n: Double? = null,
    val kg_n_generat: Double? = null,
    val estoc_final_declarat_kg_n: Double? = null
)

@Serializable
data class GranjaCampanyaBalanceUpdateRequest(
    val estoc_inicial_kg_n: Double? = null,
    val kg_n_generat: Double? = null,
    val estoc_final_declarat_kg_n: Double? = null
)
