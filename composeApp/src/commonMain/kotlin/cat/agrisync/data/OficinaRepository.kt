package cat.agrisync.data

internal class OficinaRepository(private val restClient: RestClient) {
    internal suspend fun getById(id: String): OficinaDto? {
        val q = "?select=id,nom&id=eq.$id&limit=1"
        val result: List<OficinaDto> = restClient.get("oficina", q)
        return result.firstOrNull()
    }
}

