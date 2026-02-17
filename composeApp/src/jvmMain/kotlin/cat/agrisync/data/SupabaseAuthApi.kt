package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

class SupabaseAuthApi(
    private val httpClient: HttpClient,
    private val config: SupabaseConfig.Config
) {
    @Serializable
    private data class PasswordGrantRequest(
        val email: String,
        val password: String
    )

    suspend fun signInWithPassword(email: String, password: String): AuthResponse {
        return httpClient.post {
            url("${config.url}/auth/v1/token?grant_type=password")
            contentType(ContentType.Application.Json)
            headers.append(HttpHeaders.ApiKey, config.anonKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.anonKey}")
            setBody(PasswordGrantRequest(email = email, password = password))
        }.body()
    }
}

