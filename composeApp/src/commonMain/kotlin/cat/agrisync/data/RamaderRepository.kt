package cat.agrisync.data

internal class RamaderRepository(private val restClient: RestClient) {
    private val entregaSelect =
        "?select=id,data,tipus_fertilitzant,volum_m3,kg_n_m3,kg_n,granja_origen_id,terra_desti_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya),granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom,updated_at,updated_by),terra_desti:terra_desti_id(id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,titular:titular_id(id,nif,nom_rao,telefon,email,adreca,codi_postal))"
    private val balanceSelect =
        "?select=id,granja_id,estoc_inicial_kg_n,kg_n_generat,estoc_final_declarat_kg_n,updated_at,updated_by,dan:dan_id(id,titular_id,campanya),granja:granja_id(id,titular_id,marca_oficial,nom,updated_at,updated_by)"

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
        val q = "?select=id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,updated_at,updated_by,titular:titular_id(id,nif,nom_rao,telefon,email,adreca,codi_postal)&titular_id=eq.$titularId&order=updated_at.desc"
        return restClient.get("terra", q)
    }

    internal suspend fun listAccessibleTitulars(): List<TitularDto> {
        return restClient.get("titular", "?select=id,nif,nom_rao,telefon,email,adreca,codi_postal&order=nom_rao")
    }

    internal suspend fun listAccessibleTerres(): List<TerraDto> {
        val q = "?select=id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,updated_at,updated_by,titular:titular_id(id,nif,nom_rao,telefon,email,adreca,codi_postal)&order=codi_sigpac_complet"
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

    internal suspend fun listEntreguesByTitular(titularId: String, campanya: Int): List<EntregaDejeccioDto> {
        val dan = findDanByCampanya(titularId, campanya) ?: return emptyList()
        val q = "$entregaSelect&dan_id=eq.${dan.id}&order=data.desc"
        return restClient.get("entrega_dejeccions", q)
    }

    internal suspend fun listGranjaCampanyaBalances(titularId: String, campanya: Int): List<GranjaCampanyaBalanceDto> {
        val dan = findDanByCampanya(titularId, campanya) ?: return emptyList()
        val q = "$balanceSelect&dan_id=eq.${dan.id}&order=granja_id"
        return restClient.get("granja_campanya_balance", q)
    }

    internal suspend fun listCampanyesByTitular(titularId: String): List<Int> {
        return listDansByTitular(titularId)
            .mapNotNull { it.campanya }
            .distinct()
            .sortedDescending()
    }

    internal suspend fun createEntrega(
        titularId: String,
        campanya: Int,
        granjaOrigenId: String,
        data: String,
        terraDestiId: String,
        tipusFertilitzant: String?,
        volumM3: Double,
        kgNM3: Double,
        kgN: Double
    ): EntregaDejeccioDto {
        val dan = getOrCreateDan(titularId, campanya)
        val q = entregaSelect
        val result: List<EntregaDejeccioDto> = restClient.post(
            "entrega_dejeccions",
            EntregaCreateRequest(
                dan_id = dan.id,
                granja_origen_id = granjaOrigenId,
                data = data,
                terra_desti_id = terraDestiId,
                tipus_fertilitzant = tipusFertilitzant,
                volum_m3 = volumM3,
                kg_n_m3 = kgNM3,
                kg_n = kgN
            ),
            q
        )
        return result.first()
    }

    internal suspend fun updateEntrega(id: String, body: EntregaUpdateRequest): List<EntregaDejeccioDto> {
        val q = "$entregaSelect&id=eq.$id"
        return restClient.patch("entrega_dejeccions", body, q)
    }

    internal suspend fun createGranjaCampanyaBalance(
        titularId: String,
        campanya: Int,
        granjaId: String,
        estocInicialKgN: Double?,
        kgNGenerat: Double?,
        estocFinalDeclaratKgN: Double?
    ): GranjaCampanyaBalanceDto {
        val dan = getOrCreateDan(titularId, campanya)
        val result: List<GranjaCampanyaBalanceDto> = restClient.post(
            "granja_campanya_balance",
            GranjaCampanyaBalanceCreateRequest(
                dan_id = dan.id,
                granja_id = granjaId,
                estoc_inicial_kg_n = estocInicialKgN,
                kg_n_generat = kgNGenerat,
                estoc_final_declarat_kg_n = estocFinalDeclaratKgN
            ),
            balanceSelect
        )
        return result.first()
    }

    internal suspend fun updateGranjaCampanyaBalance(
        id: String,
        body: GranjaCampanyaBalanceUpdateRequest
    ): GranjaCampanyaBalanceDto {
        val q = "$balanceSelect&id=eq.$id"
        val result: List<GranjaCampanyaBalanceDto> = restClient.patch("granja_campanya_balance", body, q)
        return result.first()
    }

    internal suspend fun deleteEntrega(id: String) {
        restClient.delete("entrega_dejeccions", "?id=eq.$id")
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
