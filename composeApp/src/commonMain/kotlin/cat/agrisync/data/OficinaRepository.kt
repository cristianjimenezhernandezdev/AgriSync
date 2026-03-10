package cat.agrisync.data

import kotlinx.serialization.Serializable

internal class OficinaRepository(private val restClient: RestClient) {
    internal suspend fun getById(id: String): OficinaDto? {
        val q = "?select=id,nom&id=eq.$id&limit=1"
        val result: List<OficinaDto> = restClient.get("oficina", q)
        return result.firstOrNull()
    }

    internal suspend fun listAll(): List<OficinaDto> {
        return restClient.get("oficina", "?select=id,nom&order=nom")
    }

    internal suspend fun create(body: OficinaCreateRequest): OficinaDto {
        val result: List<OficinaDto> = restClient.post("oficina", body)
        return result.first()
    }

    internal suspend fun update(id: String, body: OficinaUpdateRequest): OficinaDto {
        val result: List<OficinaDto> = restClient.patch("oficina", body, "?id=eq.$id")
        return result.first()
    }

    internal suspend fun delete(id: String) {
        restClient.delete("oficina", "?id=eq.$id")
    }
}

@Serializable
data class OficinaCreateRequest(
    val nom: String
)

@Serializable
data class OficinaUpdateRequest(
    val nom: String? = null
)

