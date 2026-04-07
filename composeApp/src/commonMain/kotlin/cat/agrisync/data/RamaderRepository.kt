package cat.agrisync.data

internal class RamaderRepository(private val restClient: RestClient) {
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

    internal suspend fun listGranges(titularId: String): List<GranjaDto> {
        val q = "?select=id,titular_id,marca_oficial,nom,updated_at,updated_by&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("granja", q)
    }

    internal suspend fun createGranja(body: GranjaCreateRequest): GranjaDto {
        val q = "?select=id,titular_id,marca_oficial,nom,updated_at,updated_by"
        val result: List<GranjaDto> = restClient.post("granja", body, q)
        return result.first()
    }

    internal suspend fun updateGranja(granjaId: String, body: GranjaUpdateRequest): GranjaDto {
        val q = "?id=eq.$granjaId&select=id,titular_id,marca_oficial,nom,updated_at,updated_by"
        val result: List<GranjaDto> = restClient.patch("granja", body, q)
        return result.first()
    }

    internal suspend fun deleteGranja(granjaId: String) {
        restClient.delete("granja", "?id=eq.$granjaId")
    }

    internal suspend fun listTerres(titularId: String): List<TerraDto> {
        val q = "?select=id,titular_id,codi_sigpac_complet,superficie,updated_at,updated_by&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("terra", q)
    }

    internal suspend fun listBestiarCatalog(): List<BestiarDto> {
        return restClient.get("bestiar", "?select=id,codi,descripcio&order=codi")
    }

    internal suspend fun listFaseProductivaCatalog(): List<FaseProductivaDto> {
        return restClient.get("fase_productiva", "?select=id,codi,descripcio&order=codi")
    }

    internal suspend fun listGranjaBestiar(titularId: String): List<GranjaBestiarDto> {
        val granjaIds = listGranges(titularId).map { it.id }
        if (granjaIds.isEmpty()) return emptyList()

        val ids = granjaIds.joinToString(separator = ",")
        val q = "?select=id,cens,updated_at,updated_by,granja:granja_id(id,titular_id,marca_oficial,nom,updated_at,updated_by),bestiar:bestiar_id(id,codi,descripcio),fase_productiva:fase_productiva_id(id,codi,descripcio)&granja_id=in.($ids)&order=updated_at.desc"
        return restClient.get("granja_bestiar", q)
    }

    internal suspend fun createGranjaBestiar(body: GranjaBestiarCreateRequest): GranjaBestiarDto {
        val q = "?select=id,cens,updated_at,updated_by,granja:granja_id(id,titular_id,marca_oficial,nom,updated_at,updated_by),bestiar:bestiar_id(id,codi,descripcio),fase_productiva:fase_productiva_id(id,codi,descripcio)"
        val result: List<GranjaBestiarDto> = restClient.post("granja_bestiar", body, q)
        return result.first()
    }

    internal suspend fun updateGranjaBestiar(id: String, body: GranjaBestiarUpdateRequest): List<GranjaBestiarDto> {
        val q = "?select=id,cens,updated_at,updated_by,granja:granja_id(id,titular_id,marca_oficial,nom,updated_at,updated_by),bestiar:bestiar_id(id,codi,descripcio),fase_productiva:fase_productiva_id(id,codi,descripcio)&id=eq.$id"
        return restClient.patch("granja_bestiar", body, q)
    }

    internal suspend fun deleteGranjaBestiar(id: String) {
        restClient.delete("granja_bestiar", "?id=eq.$id")
    }

    internal suspend fun listEntreguesByTitular(titularId: String): List<EntregaDejeccioDto> {
        val danIds = listDanIdsByTitular(titularId)
        if (danIds.isEmpty()) return emptyList()

        val ids = danIds.joinToString(separator = ",")
        val q = "?select=id,data,quantitat,granja_origen_id,receptor_titular_id,terra_desti_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)&dan_id=in.($ids)&order=data.desc"
        return restClient.get("entrega_dejeccions", q)
    }

    internal suspend fun createEntrega(
        titularId: String,
        granjaOrigenId: String,
        data: String,
        quantitat: Double,
        terraDestiId: String? = null,
        receptorTitularId: String? = null
    ): EntregaDejeccioDto {
        val dan = getOrCreateDan(titularId)
        val q = "?select=id,data,quantitat,granja_origen_id,receptor_titular_id,terra_desti_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)"
        val result: List<EntregaDejeccioDto> = restClient.post(
            "entrega_dejeccions",
            EntregaCreateRequest(
                dan_id = dan.id,
                granja_origen_id = granjaOrigenId,
                data = data,
                quantitat = quantitat,
                terra_desti_id = terraDestiId,
                receptor_titular_id = receptorTitularId
            ),
            q
        )
        return result.first()
    }

    internal suspend fun updateEntrega(id: String, body: EntregaUpdateRequest): List<EntregaDejeccioDto> {
        val q = "?select=id,data,quantitat,granja_origen_id,receptor_titular_id,terra_desti_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)&id=eq.$id"
        return restClient.patch("entrega_dejeccions", body, q)
    }

    internal suspend fun deleteEntrega(id: String) {
        restClient.delete("entrega_dejeccions", "?id=eq.$id")
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
