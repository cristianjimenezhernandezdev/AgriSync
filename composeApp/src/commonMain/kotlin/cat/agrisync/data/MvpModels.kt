package cat.agrisync.data

import kotlinx.serialization.Serializable

@Serializable
data class TitularAccessRow(
    val titular_id: String,
    val nom: String,
    val nif: String? = null,
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
    val updated_by: String? = null
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
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null,
    val uf: Double? = null,
    val tecnic_id: String? = null,
    val updated_at: String? = null,
    val updated_by: String? = null,
    val dan: DanRefDto? = null
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
    val quantitat: Double? = null,
    val granja_origen_id: String,
    val receptor_titular_id: String? = null,
    val terra_desti_id: String? = null,
    val updated_at: String? = null,
    val updated_by: String? = null,
    val dan: DanRefDto? = null
)

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
    val data: String,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double,
    val uf: Double
)

@Serializable
data class AplicacioUpdateRequest(
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null,
    val uf: Double? = null
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
    val quantitat: Double,
    val terra_desti_id: String? = null,
    val receptor_titular_id: String? = null
)

@Serializable
data class EntregaUpdateRequest(
    val data: String? = null,
    val quantitat: Double? = null
)
