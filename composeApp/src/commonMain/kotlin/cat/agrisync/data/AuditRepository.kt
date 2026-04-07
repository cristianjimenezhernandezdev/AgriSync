package cat.agrisync.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

internal class AuditRepository(
    private val httpClient: HttpClient,
    private val config: SupabaseConfig
) {
    internal suspend fun resolveActorLabel(userId: String?): String? {
        return resolveActorLabels(listOf(userId))[userId]
    }

    internal suspend fun resolveActorLabels(userIds: Collection<String?>): Map<String, String> {
        val ids = userIds
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .distinct()

        if (ids.isEmpty() || config.serviceRoleKey.isBlank()) return emptyMap()

        val queryIds = ids.joinToString(",")
        val response = httpClient.get {
            url("${config.url}/rest/v1/tecnic?select=user_id,nom,email&user_id=in.($queryIds)")
            contentType(ContentType.Application.Json)
            headers.append("apikey", config.serviceRoleKey)
            headers.append(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
        }

        if (!response.status.isSuccess()) return emptyMap()

        val actors: List<TecnicAuditActorDto> = response.body()
        return actors.mapNotNull { actor ->
            val userId = actor.user_id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val label = actor.email?.takeIf { it.isNotBlank() }?.let { "${actor.nom} ($it)" } ?: actor.nom
            userId to label
        }.toMap()
    }
}

@Serializable
private data class TecnicAuditActorDto(
    val user_id: String? = null,
    val nom: String,
    val email: String? = null
)
