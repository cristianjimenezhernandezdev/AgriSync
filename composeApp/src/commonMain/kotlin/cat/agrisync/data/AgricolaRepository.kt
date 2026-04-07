package cat.agrisync.data

internal class AgricolaRepository(private val restClient: RestClient) {
    internal suspend fun getTitular(titularId: String): TitularDto? {
        val q = "?select=id,nif,nom_rao,updated_at,updated_by&id=eq.$titularId&limit=1"
        val result: List<TitularDto> = restClient.get("titular", q)
        return result.firstOrNull()
    }

    internal suspend fun updateTitular(titularId: String, body: TitularUpdateRequest): TitularDto {
        val q = "?id=eq.$titularId&select=id,nif,nom_rao,updated_at,updated_by"
        val result: List<TitularDto> = restClient.patch("titular", body, q)
        return result.first()
    }

    internal suspend fun listTerres(titularId: String): List<TerraDto> {
        val q = "?select=id,titular_id,codi_sigpac_complet,superficie,updated_at,updated_by&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("terra", q)
    }

    internal suspend fun createTerra(body: TerraCreateRequest): TerraDto {
        val q = "?select=id,titular_id,codi_sigpac_complet,superficie,updated_at,updated_by"
        val result: List<TerraDto> = restClient.post("terra", body, q)
        return result.first()
    }

    internal suspend fun updateTerra(terraId: String, body: TerraUpdateRequest): TerraDto {
        val q = "?id=eq.$terraId&select=id,titular_id,codi_sigpac_complet,superficie,updated_at,updated_by"
        val result: List<TerraDto> = restClient.patch("terra", body, q)
        return result.first()
    }

    internal suspend fun deleteTerra(terraId: String) {
        restClient.delete("terra", "?id=eq.$terraId")
    }

    internal suspend fun listAplicacionsByTitular(titularId: String): List<AplicacioFertilitzantDto> {
        val danIds = listDanIdsByTitular(titularId)
        if (danIds.isEmpty()) return emptyList()

        val ids = danIds.joinToString(separator = ",")
        val q = "?select=id,data,kg_n,uf,tecnic_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)&dan_id=in.($ids)&order=data.desc"
        return restClient.get("aplicacions_fertilitzants", q)
    }

    internal suspend fun createAplicacio(
        titularId: String,
        terraId: String,
        data: String,
        kgN: Double,
        uf: Double
    ): AplicacioFertilitzantDto {
        val dan = getOrCreateDan(titularId)
        val q = "?select=id,data,kg_n,uf,tecnic_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)"
        val result: List<AplicacioFertilitzantDto> = restClient.post(
            "aplicacions_fertilitzants",
            AplicacioCreateRequest(
                dan_id = dan.id,
                terra_id = terraId,
                data = data,
                kg_n = kgN,
                uf = uf
            ),
            q
        )
        return result.first()
    }

    internal suspend fun updateAplicacio(id: String, body: AplicacioUpdateRequest): AplicacioFertilitzantDto {
        val q = "?select=id,data,kg_n,uf,tecnic_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)&id=eq.$id"
        val result: List<AplicacioFertilitzantDto> = restClient.patch("aplicacions_fertilitzants", body, q)
        return result.first()
    }

    internal suspend fun deleteAplicacio(id: String) {
        restClient.delete("aplicacions_fertilitzants", "?id=eq.$id")
    }

    private suspend fun getOrCreateDan(titularId: String): DanRefDto {
        val existing = listDansByTitular(titularId)
            .sortedByDescending { it.campanya ?: 0 }
            .firstOrNull()
        if (existing != null) return existing

        val currentYear = PlatformDateTime.currentYear()
        val q = "?select=id,titular_id,campanya"
        val result: List<DanRefDto> = restClient.post(
            "dan_declaracio",
            DanCreateRequest(
                titular_id = titularId,
                campanya = currentYear
            ),
            q
        )
        return result.first()
    }

    private suspend fun listDanIdsByTitular(titularId: String): List<String> {
        return listDansByTitular(titularId).map { it.id }
    }

    private suspend fun listDansByTitular(titularId: String): List<DanRefDto> {
        val q = "?select=id,titular_id,campanya&titular_id=eq.$titularId&order=campanya.desc"
        return restClient.get("dan_declaracio", q)
    }
}

