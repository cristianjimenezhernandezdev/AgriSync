package cat.agrisync.data

internal class AccessRepository(private val restClient: RestClient) {
    internal suspend fun listTitularAccess(): List<TitularAccessRow> {
        val q = "?select=titular_id,nom,nif,can_agricola,can_ramader,last_update_at,last_update_by&order=nom"
        return restClient.get("v_titular_access", q)
    }
}

