package cat.agrisync.data

import kotlinx.serialization.Serializable

internal class TitularManagementRepository(private val restClient: RestClient) {

    // ── Titulars ──

    internal suspend fun listAll(): List<TitularDto> {
        return restClient.get("titular", "?select=id,nif,nom_rao,created_at,created_by,updated_at,updated_by&order=nom_rao")
    }

    internal suspend fun listOficines(): List<OficinaDto> {
        return restClient.get("oficina", "?select=id,nom&order=nom")
    }

    internal suspend fun create(body: TitularCreateRequest): TitularDto {
        val result: List<TitularDto> = restClient.post("titular", body)
        return result.first()
    }

    internal suspend fun update(titularId: String, body: TitularUpdateRequest): TitularDto {
        val q = "?id=eq.$titularId"
        val result: List<TitularDto> = restClient.patch("titular", body, q)
        return result.first()
    }

    internal suspend fun delete(titularId: String) {
        restClient.delete("titular", "?id=eq.$titularId")
    }

    // ── Comparticio per oficina ──

    internal suspend fun listOfficeShares(titularId: String): List<OficinaTitularCompartitDto> {
        val q = "?select=id,oficina_id,titular_id,scope,created_at,updated_at,oficina:oficina_id(id,nom)&titular_id=eq.$titularId&order=created_at.desc"
        return restClient.get("oficina_titular_compartit", q)
    }

    internal suspend fun createOfficeShare(body: OficinaTitularCompartitCreateRequest): OficinaTitularCompartitDto {
        val q = "?select=id,oficina_id,titular_id,scope,created_at,updated_at,oficina:oficina_id(id,nom)"
        val result: List<OficinaTitularCompartitDto> = restClient.post("oficina_titular_compartit", body, q)
        return result.first()
    }

    internal suspend fun deleteOfficeShare(shareId: String) {
        restClient.delete("oficina_titular_compartit", "?id=eq.$shareId")
    }

    // ── Terres ──

    internal suspend fun listTerres(titularId: String? = null): List<TerraFullDto> {
        val filter = if (titularId != null) "&titular_id=eq.$titularId" else ""
        val q = "?select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,created_at,updated_at,titular:titular_id(id,nom_rao,nif)&order=codi_sigpac_complet$filter"
        return restClient.get("terra", q)
    }

    internal suspend fun createTerra(body: TerraCreateRequest): TerraFullDto {
        val result: List<TerraFullDto> = restClient.post(
            "terra",
            body,
            "?select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,created_at,updated_at,titular:titular_id(id,nom_rao,nif)"
        )
        return result.first()
    }

    internal suspend fun updateTerra(terraId: String, body: TerraUpdateFullRequest): TerraFullDto {
        val q = "?id=eq.$terraId&select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,created_at,updated_at,titular:titular_id(id,nom_rao,nif)"
        val result: List<TerraFullDto> = restClient.patch("terra", body, q)
        return result.first()
    }

    internal suspend fun deleteTerra(terraId: String) {
        restClient.delete("terra", "?id=eq.$terraId")
    }
}

// ── Models específics per terres ──

@Serializable
data class TerraFullDto(
    val id: String,
    val titular_id: String? = null,
    val mun_codi: String? = null,
    val poligon: Int? = null,
    val parcela: Int? = null,
    val recinte: Int? = null,
    val codi_sigpac_complet: String? = null,
    val municipi_literal: String? = null,
    val us_sigpac: String? = null,
    val cultiu: String? = null,
    val superficie: Double? = null,
    val zona: String = "ZNV",
    val limit_kg_n_ha: Double? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val titular: TitularRefDto? = null
)

@Serializable
data class TitularRefDto(
    val id: String,
    val nom_rao: String? = null,
    val nif: String? = null
)

@Serializable
data class OficinaTitularCompartitDto(
    val id: String,
    val oficina_id: String,
    val titular_id: String,
    val scope: String = "lectura",
    val created_at: String? = null,
    val updated_at: String? = null,
    val oficina: OficinaDto? = null
)

@Serializable
data class OficinaTitularCompartitCreateRequest(
    val oficina_id: String,
    val titular_id: String,
    val scope: String
)

@Serializable
data class TerraCreateRequest(
    val titular_id: String? = null,
    val mun_codi: String,
    val poligon: Int,
    val parcela: Int,
    val recinte: Int,
    val municipi_literal: String? = null,
    val us_sigpac: String? = null,
    val cultiu: String? = null,
    val superficie: Double,
    val zona: String = "ZNV"
)

@Serializable
data class TerraUpdateFullRequest(
    val titular_id: String? = null,
    val municipi_literal: String? = null,
    val us_sigpac: String? = null,
    val cultiu: String? = null,
    val superficie: Double? = null,
    val zona: String? = null
)

