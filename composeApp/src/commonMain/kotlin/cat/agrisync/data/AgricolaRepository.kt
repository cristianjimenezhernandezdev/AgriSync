package cat.agrisync.data

internal class AgricolaRepository(private val restClient: RestClient) {
    private val aplicacioSelect =
        SchemaCompatibility.agricolaAplicacioSelect

    internal suspend fun getTitular(titularId: String): TitularDto? {
        val q = "?select=id,nif,nom_rao,telefon,email,adreca,codi_postal,updated_at,updated_by&id=eq.$titularId&limit=1"
        val result: List<TitularDto> = restClient.get("titular", q)
        return result.firstOrNull()
    }

    internal suspend fun updateTitular(titularId: String, body: TitularUpdateRequest): TitularDto {
        val q = "?id=eq.$titularId&select=id,nif,nom_rao,telefon,email,adreca,codi_postal,updated_at,updated_by"
        val result: List<TitularDto> = restClient.patch("titular", body, q)
        return result.first()
    }

    internal suspend fun listTerres(titularId: String): List<TerraDto> {
        val q = "?select=id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,updated_at,updated_by&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("terra", q)
    }

    internal suspend fun createTerra(body: TerraCreateRequest): TerraDto {
        val q = "?select=id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,updated_at,updated_by"
        val result: List<TerraDto> = restClient.post("terra", body, q)
        return result.first()
    }

    internal suspend fun updateTerra(terraId: String, body: TerraUpdateRequest): TerraDto {
        val q = "?id=eq.$terraId&select=id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,updated_at,updated_by"
        val result: List<TerraDto> = restClient.patch("terra", body, q)
        return result.first()
    }

    internal suspend fun deleteTerra(terraId: String) {
        restClient.delete("terra", "?id=eq.$terraId")
    }

    internal suspend fun listAplicacionsByTitular(titularId: String, campanya: Int): List<AplicacioFertilitzantDto> {
        val dan = findDanByCampanya(titularId, campanya) ?: return emptyList()
        val q = "$aplicacioSelect&dan_id=eq.${dan.id}&order=data.desc"
        return restClient.get("aplicacions_fertilitzants", q)
    }

    internal suspend fun listCampanyesByTitular(titularId: String): List<Int> {
        return listDansByTitular(titularId)
            .mapNotNull { it.campanya }
            .distinct()
            .sortedDescending()
    }

    internal suspend fun createAplicacio(
        titularId: String,
        campanya: Int,
        terraId: String,
        data: String,
        tipusFertilitzant: String?,
        procedencia: String?,
        volumM3: Double?,
        kgNM3: Double?,
        kgN: Double
    ): AplicacioFertilitzantDto {
        val dan = getOrCreateDan(titularId, campanya)
        val q = aplicacioSelect
        val result: List<AplicacioFertilitzantDto> = restClient.post(
            "aplicacions_fertilitzants",
            AplicacioCreateRequest(
                dan_id = dan.id,
                terra_id = terraId,
                entrega_id = null,
                data = data,
                tipus_fertilitzant = tipusFertilitzant,
                procedencia = procedencia,
                volum_m3 = volumM3,
                kg_n_m3 = kgNM3,
                kg_n = kgN
            ),
            q
        )
        return result.first()
    }

    internal suspend fun updateAplicacio(id: String, body: AplicacioUpdateRequest): AplicacioFertilitzantDto {
        val q = "$aplicacioSelect&id=eq.$id"
        val result: List<AplicacioFertilitzantDto> = restClient.patch("aplicacions_fertilitzants", body, q)
        return result.first()
    }

    internal suspend fun deleteAplicacio(id: String) {
        restClient.delete("aplicacions_fertilitzants", "?id=eq.$id")
    }

    private suspend fun getOrCreateDan(titularId: String, campanya: Int): DanRefDto {
        val existing = findDanByCampanya(titularId, campanya)
        if (existing != null) return existing

        val q = "?select=id,titular_id,campanya"
        val result: List<DanRefDto> = restClient.post(
            "dan_declaracio",
            DanCreateRequest(
                titular_id = titularId,
                campanya = campanya
            ),
            q
        )
        return result.first()
    }

    private suspend fun findDanByCampanya(titularId: String, campanya: Int): DanRefDto? {
        return listDansByTitular(titularId).firstOrNull { it.campanya == campanya }
    }

    private suspend fun listDansByTitular(titularId: String): List<DanRefDto> {
        val q = "?select=id,titular_id,campanya&titular_id=eq.$titularId&order=campanya.desc"
        return restClient.get("dan_declaracio", q)
    }
}

