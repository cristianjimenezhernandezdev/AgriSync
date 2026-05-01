package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class TitularManagementRepository(
    private val restClient: RestClient,
    private val httpClient: HttpClient,
    private val config: SupabaseConfig
) {

    // ── Titulars ──

    internal suspend fun listAll(): List<TitularDto> {
        return restClient.get("titular", "?select=id,nif,nom_rao,telefon,email,adreca,codi_postal,created_at,created_by,updated_at,updated_by&order=nom_rao")
    }

    internal suspend fun listOficines(): List<OficinaDto> {
        return restClient.get("oficina", "?select=id,nom&order=nom")
    }

    internal suspend fun create(body: TitularCreateRequest): TitularDto {
        val result: List<TitularDto> = restClient.post(
            "rpc/create_titular",
            CreateTitularRpcRequest(
                p_nif = body.nif,
                p_nom_rao = body.nom_rao,
                p_telefon = body.telefon,
                p_email = body.email,
                p_adreca = body.adreca,
                p_codi_postal = body.codi_postal
            )
        )
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

    internal suspend fun findOfficeByManagerEmail(email: String): OficinaDto? {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return null
        if (config.serviceRoleKey.isBlank()) {
            throw ApiException(500, "Falta SUPABASE_SERVICE_ROLE_KEY per buscar oficines per email")
        }

        val response = httpClient.get {
            url("${config.url}/rest/v1/tecnic?select=oficina:oficina_id(id,nom)&email=eq.$cleanEmail&rol=eq.oficina_manager&actiu=eq.true&limit=1")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
        }

        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText().ifBlank { "HTTP ${response.status.value}" }
            throw ApiException(response.status.value, msg)
        }

        val result: List<TecnicOfficeLookupDto> = response.body()
        return result.firstOrNull()?.oficina
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
        val q = "?select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,created_at,updated_at,titular:titular_id(id,nom_rao,nif,telefon,email,adreca,codi_postal)&order=codi_sigpac_complet$filter"
        return restClient.get("terra", q)
    }

    internal suspend fun createTerra(body: TerraCreateRequest): TerraFullDto {
        val result: List<TerraFullDto> = restClient.post(
            "rpc/create_terra",
            CreateTerraRpcRequest(
                p_mun_codi = body.mun_codi,
                p_poligon = body.poligon,
                p_parcela = body.parcela,
                p_recinte = body.recinte,
                p_superficie = body.superficie,
                p_titular_id = body.titular_id,
                p_municipi_literal = body.municipi_literal,
                p_us_sigpac = body.us_sigpac,
                p_cultiu = body.cultiu,
                p_zona = body.zona
            ),
            "?select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,created_at,updated_at"
        )
        return result.first()
    }

    internal suspend fun updateTerra(terraId: String, body: TerraUpdateFullRequest): TerraFullDto {
        val q = "?id=eq.$terraId&select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,created_at,updated_at,titular:titular_id(id,nom_rao,nif,telefon,email,adreca,codi_postal)"
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
    val nif: String? = null,
    val telefon: String? = null,
    val email: String? = null,
    val adreca: String? = null,
    val codi_postal: String? = null
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
private data class TecnicOfficeLookupDto(
    val oficina: OficinaDto? = null
)

@Serializable
private data class CreateTitularRpcRequest(
    val p_nif: String? = null,
    val p_nom_rao: String,
    val p_telefon: String? = null,
    val p_email: String? = null,
    val p_adreca: String? = null,
    val p_codi_postal: String? = null
)

@Serializable
data class CreateTerraRpcRequest(
    val p_mun_codi: String,
    val p_poligon: Int,
    val p_parcela: Int,
    val p_recinte: Int,
    val p_superficie: Double,
    val p_titular_id: String? = null,
    val p_municipi_literal: String? = null,
    val p_us_sigpac: String? = null,
    val p_cultiu: String? = null,
    val p_zona: String = "ZNV"
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

