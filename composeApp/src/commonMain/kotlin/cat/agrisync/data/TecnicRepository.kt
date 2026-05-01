package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class TecnicRepository(
    private val restClient: RestClient,
    private val httpClient: HttpClient,
    private val config: SupabaseConfig
) {
    /** Llista tots els tècnics (admin veu tots, manager veu oficina) */
    internal suspend fun listAll(): List<TecnicDto> {
        return restClient.get(
            "tecnic",
            "?select=id,oficina_id,user_id,nom,email,telefon,rol,actiu,created_at,created_by,updated_at,updated_by&order=nom"
        )
    }

    /** Llista oficines */
    internal suspend fun listOficines(): List<OficinaDto> {
        return restClient.get("oficina", "?select=id,nom&order=nom")
    }

    /** Actualitza un tècnic existent */
    internal suspend fun updateTecnic(tecnicId: String, body: TecnicUpdateRequest): TecnicDto {
        val q = "?id=eq.$tecnicId"
        val result: List<TecnicDto> = restClient.patch("tecnic", body, q)
        return result.first()
    }

    /** Crea un tècnic nou a public.tecnic (sense credencials Auth) */
    internal suspend fun createTecnic(body: TecnicCreateRequest): TecnicDto {
        val result: List<TecnicDto> = restClient.post("tecnic", body)
        return result.first()
    }

    /** Crea el perfil de tècnic amb service_role per evitar bloquejos RLS en altes administratives. */
    internal suspend fun createTecnicWithServiceRole(body: TecnicCreateRequest): TecnicDto {
        if (config.serviceRoleKey.isBlank()) {
            throw ApiException(500, "Falta SUPABASE_SERVICE_ROLE_KEY per crear el perfil del tecnic")
        }

        val response = httpClient.post {
            url("${config.url}/rest/v1/tecnic")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
            headers.append("Prefer", "return=representation")
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText().ifBlank { "HTTP ${response.status.value}" }
            throw ApiException(response.status.value, msg)
        }

        val result: List<TecnicDto> = response.body()
        return result.first()
    }

    /** Cerca un tècnic visible per email per prevenir duplicats evidents abans de tocar Auth. */
    internal suspend fun findVisibleTecnicByEmail(email: String): TecnicDto? {
        val cleanEmail = email.trim().lowercase()
        val result: List<TecnicDto> = restClient.get(
            "tecnic",
            "?select=id,oficina_id,user_id,nom,email,telefon,rol,actiu,created_at,created_by,updated_at,updated_by&email=eq.$cleanEmail&limit=1"
        )
        return result.firstOrNull()
    }

    /** Crea un usuari Auth via Admin API (requereix service_role key) i retorna si s'ha creat o recuperat. */
    internal suspend fun createAuthUser(email: String, password: String): AuthUserLinkResult {
        val cleanEmail = email.trim().lowercase()
        val response = httpClient.post {
            url("${config.url}/auth/v1/admin/users")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
            setBody(CreateAuthUserRequest(
                email = cleanEmail,
                password = password,
                email_confirm = true
            ))
        }

        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText()
            if (response.status.value == 422 && msg.contains("email_exists", ignoreCase = true)) {
                val existingUserId = recoverExistingAuthUserId(cleanEmail, password)
                if (existingUserId != null) {
                    return AuthUserLinkResult(userId = existingUserId, reusedExisting = true)
                }
                throw ApiException(
                    response.status.value,
                    "Ja existeix un login amb aquest email a Supabase Auth. " +
                        "Pot ser un intent anterior a mig crear o un usuari ja existent amb un altre password."
                )
            }
            throw ApiException(response.status.value, "Error creant usuari Auth: $msg")
        }

        val result: AuthUserCreatedResponse = response.body()
        return AuthUserLinkResult(userId = result.id, reusedExisting = false)
    }

    /** Llista assignacions tecnic_titular d'un tècnic */
    internal suspend fun listAssignacions(tecnicId: String): List<TecnicTitularWithTitular> {
        val q = "?select=id,tecnic_id,titular_id,scope,actiu,titular:titular_id(id,nif,nom_rao,telefon,email,adreca,codi_postal)&tecnic_id=eq.$tecnicId&order=created_at.desc"
        return restClient.get("tecnic_titular", q)
    }

    internal suspend fun listCollaboratingTecnicsByTitular(titularId: String): List<TitularCollaboratingTecnicDto> {
        val q = "?select=id,scope,actiu,tecnic:tecnic_id(id,oficina_id,user_id,nom,email,telefon,rol,actiu,oficina:oficina_id(id,nom))&titular_id=eq.$titularId&actiu=eq.true&order=created_at.asc"
        return restClient.get("tecnic_titular", q)
    }

    internal suspend fun listCollaboratingOficinesByTitular(titularId: String): List<TitularSharedOfficeDto> {
        val q = "?select=id,scope,oficina:oficina_id(id,nom)&titular_id=eq.$titularId&order=created_at.asc"
        return restClient.get("oficina_titular_compartit", q)
    }

    /** Llista tots els titulars (per poder assignar) */
    internal suspend fun listAllTitulars(): List<TitularDto> {
        return restClient.get("titular", "?select=id,nif,nom_rao,telefon,email,adreca,codi_postal&order=nom_rao")
    }

    /** Crea una assignació tecnic_titular */
    internal suspend fun createAssignacio(body: TecnicTitularAssignRequest): TecnicTitularDto {
        val result: List<TecnicTitularDto> = restClient.post("tecnic_titular", body)
        return result.first()
    }

    /** Elimina una assignació tecnic_titular */
    internal suspend fun deleteAssignacio(assignacioId: String) {
        restClient.delete("tecnic_titular", "?id=eq.$assignacioId")
    }

    /** Elimina un tècnic de public.tecnic. Les assignacions tecnic_titular cauen per cascade. */
    internal suspend fun deleteTecnic(tecnicId: String) {
        restClient.delete("tecnic", "?id=eq.$tecnicId")
    }

    /** Canvia el password d'un usuari Auth via Admin API (requereix service_role key) */
    internal suspend fun updateAuthUserPassword(userId: String, newPassword: String) {
        val response = httpClient.put {
            url("${config.url}/auth/v1/admin/users/$userId")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
            setBody(UpdatePasswordRequest(password = newPassword))
        }

        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText()
            throw ApiException(response.status.value, "Error canviant password: $msg")
        }
    }

    /** Elimina un usuari Auth via Admin API (requereix service_role key). */
    internal suspend fun deleteAuthUser(userId: String) {
        val response = httpClient.delete {
            url("${config.url}/auth/v1/admin/users/$userId")
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
        }

        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText()
            throw ApiException(response.status.value, "Error eliminant usuari Auth: $msg")
        }
    }

    /** Resol el nom del tecnic que correspon a un auth.uid() guardat a updated_by/created_by. */
    internal suspend fun resolveActorLabel(userId: String?): String? {
        val cleanUserId = userId?.trim().orEmpty()
        if (cleanUserId.isBlank() || config.serviceRoleKey.isBlank()) return null

        val response = httpClient.get {
            url("${config.url}/rest/v1/tecnic?select=user_id,nom,email&user_id=eq.$cleanUserId&limit=1")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
        }

        if (!response.status.isSuccess()) {
            return null
        }

        val result: List<TecnicActorLookupDto> = response.body()
        val actor = result.firstOrNull() ?: return null
        return actor.email?.takeIf { it.isNotBlank() }?.let { "${actor.nom} ($it)" } ?: actor.nom
    }

    private suspend fun recoverExistingAuthUserId(email: String, password: String): String? {
        val response = httpClient.post {
            url("${config.url}/auth/v1/token?grant_type=password")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
            setBody(PasswordGrantRequest(email = email, password = password))
        }

        if (!response.status.isSuccess()) {
            return null
        }

        val authResponse: AuthResponse = response.body()
        return authResponse.user.id
    }
}

@Serializable
data class TecnicCreateRequest(
    val oficina_id: String,
    val user_id: String? = null,
    val nom: String,
    val email: String? = null,
    val telefon: String? = null,
    val rol: String = "tecnic",
    val actiu: Boolean = true
)

@Serializable
data class TecnicUpdateRequest(
    val nom: String? = null,
    val email: String? = null,
    val telefon: String? = null,
    val rol: String? = null,
    val actiu: Boolean? = null,
    val oficina_id: String? = null
)

@Serializable
    private data class CreateAuthUserRequest(
    val email: String,
    val password: String,
    val email_confirm: Boolean = true
)

@Serializable
private data class PasswordGrantRequest(
    val email: String,
    val password: String
)

@Serializable
private data class AuthUserCreatedResponse(
    val id: String,
    val email: String? = null
)

data class AuthUserLinkResult(
    val userId: String,
    val reusedExisting: Boolean
)

@Serializable
data class TecnicTitularWithTitular(
    val id: String,
    val tecnic_id: String,
    val titular_id: String,
    val scope: String = "comu",
    val actiu: Boolean = true,
    val titular: TitularDto? = null
)

@Serializable
data class OficinaEmbeddedDto(
    val id: String,
    val nom: String
)

@Serializable
data class TecnicCollaborationDto(
    val id: String,
    val oficina_id: String,
    val user_id: String? = null,
    val nom: String,
    val email: String? = null,
    val telefon: String? = null,
    val rol: String? = null,
    val actiu: Boolean = true,
    val oficina: OficinaEmbeddedDto? = null
)

@Serializable
data class TitularCollaboratingTecnicDto(
    val id: String,
    val scope: String = "comu",
    val actiu: Boolean = true,
    val tecnic: TecnicCollaborationDto? = null
)

@Serializable
data class TitularSharedOfficeDto(
    val id: String,
    val scope: String = "lectura",
    val oficina: OficinaEmbeddedDto? = null
)

@Serializable
private data class UpdatePasswordRequest(
    val password: String
)

@Serializable
private data class TecnicActorLookupDto(
    val user_id: String? = null,
    val nom: String,
    val email: String? = null
)

