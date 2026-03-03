package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.statement.bodyAsText

internal class RestClient(
    private val httpClient: HttpClient,
    private val config: SupabaseConfig,
    private val sessionProvider: () -> Session?
) {
    private fun authHeaderValue(): String {
        val token = sessionProvider()?.accessToken ?: config.anonKey
        return "Bearer $token"
    }

    private fun baseUrl(path: String, query: String?): String {
        val suffix = query?.trim().orEmpty()
        return "${config.url}/rest/v1/$path$suffix"
    }

    private suspend inline fun <reified T> handle(response: io.ktor.client.statement.HttpResponse): T {
        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText().ifBlank { "HTTP ${response.status.value}" }
            throw ApiException(response.status.value, msg)
        }
        return response.body()
    }

    internal suspend inline fun <reified T> get(path: String, query: String? = null): T {
        val response = httpClient.get {
            url(baseUrl(path, query))
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, authHeaderValue())
        }
        return handle(response)
    }

    /** GET amb token explicit (per a crides durant el login, abans que la sessio es guardi). */
    internal suspend inline fun <reified T> get(path: String, query: String? = null, accessToken: String): T {
        val response = httpClient.get {
            url(baseUrl(path, query))
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        return handle(response)
    }

    internal suspend inline fun <reified T> post(path: String, body: Any, query: String? = null): T {
        val response = httpClient.post {
            url(baseUrl(path, query))
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, authHeaderValue())
            headers.append("Prefer", "return=representation")
            setBody(body)
        }
        return handle(response)
    }

    internal suspend inline fun <reified T> patch(path: String, body: Any, query: String? = null): T {
        val response = httpClient.patch {
            url(baseUrl(path, query))
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, authHeaderValue())
            headers.append("Prefer", "return=representation")
            setBody(body)
        }
        return handle(response)
    }

    internal suspend fun delete(path: String, query: String? = null) {
        val response = httpClient.delete {
            url(baseUrl(path, query))
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.anonKey)
            headers.append(HttpHeaders.Authorization, authHeaderValue())
        }
        if (!response.status.isSuccess()) {
            val msg = response.bodyAsText().ifBlank { "HTTP ${response.status.value}" }
            throw ApiException(response.status.value, msg)
        }
    }
}
