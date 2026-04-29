package cat.agrisync.data

import kotlinx.serialization.Serializable

internal class DanPreparationRepository(private val restClient: RestClient) {
    private val aplicacioSelect =
        "?select=id,data,tipus_fertilitzant,procedencia,volum_m3,kg_n_m3,kg_n,terra:terra_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha),dan:dan_id(id,titular_id,campanya)"
    private val entregaSelect =
        "?select=id,data,tipus_fertilitzant,volum_m3,kg_n_m3,kg_n,granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom),terra_desti:terra_desti_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha),dan:dan_id(id,titular_id,campanya)"
    private val balanceSelect =
        "?select=id,granja_id,estoc_inicial_kg_n,kg_n_generat,estoc_final_declarat_kg_n,granja:granja_id(id,titular_id,marca_oficial,nom),dan:dan_id(id,titular_id,campanya)"

    internal suspend fun getTitular(titularId: String): TitularDto? {
        val q = "?select=id,nif,nom_rao,telefon,email,adreca,codi_postal,updated_at,updated_by&id=eq.$titularId&limit=1"
        val result: List<TitularDto> = restClient.get("titular", q)
        return result.firstOrNull()
    }

    internal suspend fun listTerres(titularId: String): List<DanPreparationTerraDto> {
        val q = "?select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha&titular_id=eq.$titularId&order=codi_sigpac_complet"
        return restClient.get("terra", q)
    }

    internal suspend fun listAplicacionsByTitular(titularId: String, campanya: Int): List<DanPreparationAplicacioDto> {
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

    internal suspend fun listGranges(titularId: String): List<GranjaDto> {
        val q = "?select=id,titular_id,marca_oficial,nom&titular_id=eq.$titularId&order=marca_oficial"
        return restClient.get("granja", q)
    }

    internal suspend fun listGranjaBestiar(titularId: String): List<GranjaBestiarDto> {
        val granjaIds = listGranges(titularId).map { it.id }
        if (granjaIds.isEmpty()) return emptyList()

        val ids = granjaIds.joinToString(separator = ",")
        val q = "?select=id,cens,granja:granja_id(id,titular_id,marca_oficial,nom),bestiar:bestiar_id(id,codi,descripcio),fase_productiva:fase_productiva_id(id,codi,descripcio)&granja_id=in.($ids)&order=updated_at.desc"
        return restClient.get("granja_bestiar", q)
    }

    internal suspend fun listEntreguesByTitular(titularId: String, campanya: Int): List<DanPreparationEntregaDto> {
        val dan = findDanByCampanya(titularId, campanya) ?: return emptyList()
        val q = "$entregaSelect&dan_id=eq.${dan.id}&order=data.desc"
        return restClient.get("entrega_dejeccions", q)
    }

    internal suspend fun listGranjaCampanyaBalances(titularId: String, campanya: Int): List<DanPreparationGranjaBalanceDto> {
        val dan = findDanByCampanya(titularId, campanya) ?: return emptyList()
        val q = "$balanceSelect&dan_id=eq.${dan.id}&order=granja_id"
        return restClient.get("granja_campanya_balance", q)
    }

    private suspend fun findDanByCampanya(titularId: String, campanya: Int): DanRefDto? {
        return listDansByTitular(titularId).firstOrNull { it.campanya == campanya }
    }

    private suspend fun listDansByTitular(titularId: String): List<DanRefDto> {
        val q = "?select=id,titular_id,campanya&titular_id=eq.$titularId&order=campanya.desc"
        return restClient.get("dan_declaracio", q)
    }
}

@Serializable
internal data class DanPreparationTerraDto(
    val id: String,
    val titular_id: String? = null,
    val mun_codi: String? = null,
    val poligon: Int? = null,
    val parcela: Int? = null,
    val recinte: Int? = null,
    val codi_sigpac_complet: String? = null,
    val municipi_literal: String? = null,
    val us_sigpac: String? = null,
    val cultiu: String? = null,
    val superficie: Double? = null,
    val zona: String = "ZNV",
    val limit_kg_n_ha: Double? = null
)

@Serializable
internal data class DanPreparationAplicacioDto(
    val id: String,
    val entrega_id: String? = null,
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val procedencia: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null,
    val entrega: EntregaAplicacioLinkDto? = null,
    val terra: DanPreparationTerraDto? = null,
    val dan: DanRefDto? = null
)

@Serializable
internal data class DanPreparationEntregaDto(
    val id: String,
    val data: String? = null,
    val tipus_fertilitzant: String? = null,
    val volum_m3: Double? = null,
    val kg_n_m3: Double? = null,
    val kg_n: Double? = null,
    val granja_origen: GranjaDto? = null,
    val terra_desti: DanPreparationTerraDto? = null,
    val dan: DanRefDto? = null
)

@Serializable
internal data class DanPreparationGranjaBalanceDto(
    val id: String,
    val granja_id: String,
    val estoc_inicial_kg_n: Double? = null,
    val kg_n_generat: Double? = null,
    val estoc_final_declarat_kg_n: Double? = null,
    val granja: GranjaDto? = null,
    val dan: DanRefDto? = null
)
