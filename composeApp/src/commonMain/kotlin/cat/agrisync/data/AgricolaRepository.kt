package cat.agrisync.data

internal class AgricolaRepository(private val restClient: RestClient) {
    internal suspend fun getTitular(titularId: String): TitularDto? {
        val q = "?select=id,nif,nom_rao,updated_at,updated_by&id=eq.$titularId&limit=1"
        val result: List<TitularDto> = restClient.get("titular", q)
        return result.firstOrNull()
    }

    internal suspend fun listTerres(titularId: String): List<TerraDto> {
        val q = "?select=id,titular_id,codi_sigpac_complet,superficie&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("terra", q)
    }

    internal suspend fun listAplicacionsByTitular(titularId: String): List<AplicacioFertilitzantDto> {
        val danIds = listDanIdsByTitular(titularId)
        if (danIds.isEmpty()) return emptyList()

        val ids = danIds.joinToString(separator = ",")
        val q = "?select=id,data,kg_n,uf,tecnic_id,dan:dan_id(id,titular_id,campanya)&dan_id=in.($ids)&order=data.desc"
        return restClient.get("aplicacions_fertilitzants", q)
    }

    private suspend fun listDanIdsByTitular(titularId: String): List<String> {
        val q = "?select=id&titular_id=eq.$titularId"
        val rows: List<DanIdRow> = restClient.get("dan_declaracio", q)
        return rows.map { it.id }
    }

    @kotlinx.serialization.Serializable
    private data class DanIdRow(val id: String)
}

