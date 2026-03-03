package cat.agrisync.data

internal class RamaderRepository(private val restClient: RestClient) {
    internal suspend fun getTitular(titularId: String): TitularDto? {
        val q = "?select=id,nif,nom_rao,updated_at,updated_by&id=eq.$titularId&limit=1"
        val result: List<TitularDto> = restClient.get("titular", q)
        return result.firstOrNull()
    }

    internal suspend fun updateTitular(titularId: String, body: TitularUpdateRequest): TitularDto {
        val q = "?id=eq.$titularId"
        val result: List<TitularDto> = restClient.patch("titular", body, q)
        return result.first()
    }

    internal suspend fun listGranges(titularId: String): List<GranjaDto> {
        val q = "?select=id,titular_id,marca_oficial,nom&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("granja", q)
    }

    internal suspend fun updateGranja(granjaId: String, body: GranjaUpdateRequest): GranjaDto {
        val q = "?id=eq.$granjaId"
        val result: List<GranjaDto> = restClient.patch("granja", body, q)
        return result.first()
    }

    internal suspend fun listGranjaBestiar(titularId: String): List<GranjaBestiarDto> {
        val granjaIds = listGranges(titularId).map { it.id }
        if (granjaIds.isEmpty()) return emptyList()

        val ids = granjaIds.joinToString(separator = ",")
        val q = "?select=id,cens,granja:granja_id(id,titular_id,marca_oficial,nom),bestiar:bestiar_id(id,codi,descripcio),fase_productiva:fase_productiva_id(id,codi,descripcio)&granja_id=in.($ids)&order=updated_at.desc"
        return restClient.get("granja_bestiar", q)
    }

    internal suspend fun updateGranjaBestiar(id: String, body: GranjaBestiarUpdateRequest): List<GranjaBestiarDto> {
        val q = "?select=id,cens,granja:granja_id(id,titular_id,marca_oficial,nom),bestiar:bestiar_id(id,codi,descripcio),fase_productiva:fase_productiva_id(id,codi,descripcio)&id=eq.$id"
        return restClient.patch("granja_bestiar", body, q)
    }

    internal suspend fun listEntreguesByTitular(titularId: String): List<EntregaDejeccioDto> {
        val danIds = listDanIdsByTitular(titularId)
        if (danIds.isEmpty()) return emptyList()

        val ids = danIds.joinToString(separator = ",")
        val q = "?select=id,data,quantitat,granja_origen_id,receptor_titular_id,terra_desti_id,dan:dan_id(id,titular_id,campanya)&dan_id=in.($ids)&order=data.desc"
        return restClient.get("entrega_dejeccions", q)
    }

    internal suspend fun updateEntrega(id: String, body: EntregaUpdateRequest): List<EntregaDejeccioDto> {
        val q = "?select=id,data,quantitat,granja_origen_id,receptor_titular_id,terra_desti_id,dan:dan_id(id,titular_id,campanya)&id=eq.$id"
        return restClient.patch("entrega_dejeccions", body, q)
    }

    private suspend fun listDanIdsByTitular(titularId: String): List<String> {
        val q = "?select=id&titular_id=eq.$titularId"
        val rows: List<DanIdRow> = restClient.get("dan_declaracio", q)
        return rows.map { it.id }
    }

    @kotlinx.serialization.Serializable
    private data class DanIdRow(val id: String)
}
