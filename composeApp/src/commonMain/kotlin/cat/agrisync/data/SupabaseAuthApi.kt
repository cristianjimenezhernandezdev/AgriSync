package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
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

    suspend fun getMyTecnic(accessToken: String): TecnicDto? {
        val response = httpClient.post {
            url("${config.url}/rest/v1/rpc/get_my_tecnic")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
            setBody("{}")  // RPC sense arguments
        }

        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText().ifBlank { "HTTP ${response.status.value}" }
            throw ApiException(response.status.value, msg)
        }

        val list: List<TecnicDto> = response.body()
        return list.firstOrNull()
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
