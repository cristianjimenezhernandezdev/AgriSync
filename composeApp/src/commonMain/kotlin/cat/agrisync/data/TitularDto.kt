package cat.agrisync.data

import kotlinx.serialization.Serializable

@Serializable
data class TitularDto(
    val id: String,
    val nif: String? = null,
    val nom_rao: String,
    val telefon: String? = null,
    val email: String? = null,
    val adreca: String? = null,
    val codi_postal: String? = null,
    val created_at: String? = null,
    val created_by: String? = null,
    val updated_at: String? = null,
    val updated_by: String? = null
)

@Serializable
data class TitularCreateRequest(
    val nif: String? = null,
    val nom_rao: String,
    val telefon: String? = null,
    val email: String? = null,
    val adreca: String? = null,
    val codi_postal: String? = null
)

@Serializable
data class TitularUpdateRequest(
    val nif: String? = null,
    val nom_rao: String,
    val telefon: String? = null,
    val email: String? = null,
    val adreca: String? = null,
    val codi_postal: String? = null
)

/** Wrapper per la resposta anidada de tecnic_titular amb select=titular:titular_id(...) */
@Serializable
data class TecnicTitularRow(
    val titular: TitularDto
)

@Serializable
data class TecnicTitularDto(
    val id: String,
    val tecnic_id: String,
    val titular_id: String,
    val scope: String = "comu",
    val actiu: Boolean = true,
    val created_at: String? = null,
    val created_by: String? = null
)

@Serializable
data class TecnicTitularWithTecnic(
    val id: String,
    val tecnic_id: String,
    val titular_id: String,
    val scope: String = "comu",
    val actiu: Boolean = true,
    val tecnic: TecnicDto? = null
)

@Serializable
data class TecnicTitularAssignRequest(
    val tecnic_id: String,
    val titular_id: String,
    val scope: String = "comu",
    val actiu: Boolean = true
)

@Serializable
data class OficinaDto(
    val id: String,
    val nom: String
)
