package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class SupabaseAuthApi(
    private val httpClient: HttpClient,
    private val config: SupabaseConfig
) {
    @Serializable
    private data class PasswordGrantRequest(
        val email: String,
        val password: String
    )

    @Serializable
    private data class RefreshGrantRequest(
        val refresh_token: String
    )

    @Serializable
    private data class AuthErrorResponse(
        val error: String? = null,
        val error_description: String? = null,
        val msg: String? = null,
        val message: String? = null
    )

    suspend fun signInWithPassword(email: String, password: String): AuthResponse {
        val authUrl = "${config.url}/auth/v1/token?grant_type=password"
        println("[AUTH-API] POST $authUrl")
        println("[AUTH-API] apikey=${config.anonKey.take(20)}...")
        println("[AUTH-API] email=$email")

        val response = httpClient.post {
            url(authUrl)
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
            setBody(PasswordGrantRequest(email = email, password = password))
        }

        println("[AUTH-API] Response status: ${response.status}")
        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            println("[AUTH-API] Error body: $body")
        }

        return parseAuthResponse(response)
    }

    suspend fun refresh(refreshToken: String): AuthResponse {
        val response = httpClient.post {
            url("${config.url}/auth/v1/token?grant_type=refresh_token")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
            setBody(RefreshGrantRequest(refresh_token = refreshToken))
        }
        return parseAuthResponse(response)
    }

    suspend fun getMyTecnic(accessToken: String, loginEmail: String? = null): TecnicDto? {
        // Intent 1: RPC get_my_tecnic (SECURITY DEFINER)
        try {
            val response = httpClient.post {
                url("${config.url}/rest/v1/rpc/get_my_tecnic")
                contentType(ContentType.Application.Json)
                headers.append("apikey", config.anonKey)
                headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody("{}")  // RPC sense arguments
            }

            if (response.status.isSuccess()) {
                val list: List<TecnicDto> = response.body()
                if (list.isNotEmpty()) {
                    println("[AUTH-API] getMyTecnic via RPC OK: ${list.first().nom}")
                    return list.first()
                }
            } else {
                val msg = response.bodyAsText()
                println("[AUTH-API] RPC get_my_tecnic failed: ${response.status} - $msg")
            }
        } catch (ex: Exception) {
            println("[AUTH-API] RPC get_my_tecnic exception: ${ex.message}")
        }

        // Intent 2: consulta directa amb el mateix token de l'usuari.
        val userId = extractUserIdFromToken(accessToken)
        if (userId != null) {
            println("[AUTH-API] Fallback: buscant tecnic per user_id=$userId amb token usuari")
            try {
                val response = httpClient.get {
                    url("${config.url}/rest/v1/tecnic?user_id=eq.$userId&limit=1")
                    contentType(ContentType.Application.Json)
                    headers.append("apikey", config.anonKey)
                    headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                if (response.status.isSuccess()) {
                    val list: List<TecnicDto> = response.body()
                    if (list.isNotEmpty()) {
                        println("[AUTH-API] Fallback OK: ${list.first().nom}")
                        return list.first()
                    }
                }
            } catch (ex: Exception) {
                println("[AUTH-API] Fallback exception: ${ex.message}")
            }
        }

        // Intent 3: Buscar per EMAIL amb service_role_key (bypass RLS).
        // Si el user_id no coincideix, l'autocorregim.
        val email = loginEmail ?: extractEmailFromToken(accessToken)
        if (email != null && config.serviceRoleKey.isNotBlank()) {
            println("[AUTH-API] Fallback email: buscant tecnic per email=$email amb service_role_key")
            try {
                val response = httpClient.get {
                    url("${config.url}/rest/v1/tecnic?email=eq.$email&limit=1")
                    contentType(ContentType.Application.Json)
                    headers.append("apikey", config.serviceRoleKey)
                    headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
                }
                if (response.status.isSuccess()) {
                    val list: List<TecnicDto> = response.body()
                    if (list.isNotEmpty()) {
                        val tecnic = list.first()
                        println("[AUTH-API] Fallback email OK: ${tecnic.nom} (user_id actual=${tecnic.user_id})")

                        // Auto-fix: si el user_id no coincideix, actualitzar-lo
                        if (userId != null && tecnic.user_id != userId) {
                            println("[AUTH-API] Auto-fix: actualitzant user_id de ${tecnic.user_id} a $userId")
                            try {
                                val patchResp = httpClient.patch {
                                    url("${config.url}/rest/v1/tecnic?id=eq.${tecnic.id}")
                                    contentType(ContentType.Application.Json)
                                    headers.append("apikey", config.serviceRoleKey)
                                    headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
                                    headers.append("Prefer", "return=representation")
                                    setBody("""{"user_id":"$userId"}""")
                                }
                                if (patchResp.status.isSuccess()) {
                                    val updated: List<TecnicDto> = patchResp.body()
                                    if (updated.isNotEmpty()) {
                                        println("[AUTH-API] Auto-fix OK! user_id actualitzat.")
                                        return updated.first()
                                    }
                                } else {
                                    println("[AUTH-API] Auto-fix PATCH failed: ${patchResp.status} - ${patchResp.bodyAsText()}")
                                }
                            } catch (patchEx: Exception) {
                                println("[AUTH-API] Auto-fix exception: ${patchEx.message}")
                            }
                        }
                        return tecnic
                    }
                }
            } catch (ex: Exception) {
                println("[AUTH-API] Fallback email exception: ${ex.message}")
            }
        }

        return null
    }

    /** Extreu el user_id (sub) del JWT sense verificar signatura */
    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun extractUserIdFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = kotlin.io.encoding.Base64.UrlSafe.decode(padded.encodeToByteArray())
            val json = decoded.decodeToString()
            val regex = """"sub"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(json)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    /** Extreu l'email del JWT sense verificar signatura */
    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun extractEmailFromToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = kotlin.io.encoding.Base64.UrlSafe.decode(padded.encodeToByteArray())
            val json = decoded.decodeToString()
            val regex = """"email"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(json)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun parseAuthResponse(response: io.ktor.client.statement.HttpResponse): AuthResponse {
        if (!response.status.isSuccess()) {
            val errorMsg = try {
                val err: AuthErrorResponse = response.body()
                err.error_description ?: err.msg ?: err.message ?: err.error ?: "Error d'autenticacio"
            } catch (_: Exception) {
                response.bodyAsText().ifBlank { "HTTP ${response.status.value}" }
            }
            throw ApiException(response.status.value, errorMsg)
        }

        return response.body()
    }
}
