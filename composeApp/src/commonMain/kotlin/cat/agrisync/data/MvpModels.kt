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
    val superficie: Double? = null
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
    val data: String? = null,
    val kg_n: Double? = null,
    val uf: Double? = null,
    val tecnic_id: String? = null,
    val dan: DanRefDto? = null
)

@Serializable
data class GranjaDto(
    val id: String,
    val titular_id: String,
    val marca_oficial: String,
    val nom: String? = null
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
    val dan: DanRefDto? = null
)

