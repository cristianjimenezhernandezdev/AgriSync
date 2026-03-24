package cat.agrisync.data

import kotlinx.serialization.Serializable

internal class DanPreparationRepository(private val restClient: RestClient) {

    internal suspend fun getTitular(titularId: String): TitularDto? {
        val q = "?select=id,nif,nom_rao,updated_at,updated_by&id=eq.$titularId&limit=1"
        val result: List<TitularDto> = restClient.get("titular", q)
        return result.firstOrNull()
    }

    internal suspend fun listTerres(titularId: String): List<DanPreparationTerraDto> {
        val q = "?select=id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,superficie&titular_id=eq.$titularId&order=codi_sigpac_complet"
        return restClient.get("terra", q)
    }

    internal suspend fun listAplicacionsByTitular(titularId: String): List<DanPreparationAplicacioDto> {
        val danIds = listDanIdsByTitular(titularId)
        if (danIds.isEmpty()) return emptyList()

        val ids = danIds.joinToString(separator = ",")
        val q = "?select=id,data,kg_n,uf,terra:terra_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,superficie),dan:dan_id(id,titular_id,campanya)&dan_id=in.($ids)&order=data.desc"
        return restClient.get("aplicacions_fertilitzants", q)
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

    internal suspend fun listEntreguesByTitular(titularId: String): List<DanPreparationEntregaDto> {
        val danIds = listDanIdsByTitular(titularId)
        if (danIds.isEmpty()) return emptyList()

        val ids = danIds.joinToString(separator = ",")
        val q = "?select=id,data,quantitat,granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom),receptor_titular:receptor_titular_id(id,nif,nom_rao),terra_desti:terra_desti_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,superficie),dan:dan_id(id,titular_id,campanya)&dan_id=in.($ids)&order=data.desc"
        return restClient.get("entrega_dejeccions", q)
    }

    private suspend fun listDanIdsByTitular(titularId: String): List<String> {
        val q = "?select=id&titular_id=eq.$titularId"
        val rows: List<DanIdRow> = restClient.get("dan_declaracio", q)
        return rows.map { it.id }
    }

    @Serializable
    private data class DanIdRow(val id: String)
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
    val superficie: Double? = null
)

@Serializable
internal data class DanPreparationAplicacioDto(
    val id: String,
    val data: String? = null,
    val kg_n: Double? = null,
    val uf: Double? = null,
    val terra: DanPreparationTerraDto? = null,
    val dan: DanRefDto? = null
)

@Serializable
internal data class DanPreparationEntregaDto(
    val id: String,
    val data: String? = null,
    val quantitat: Double? = null,
    val granja_origen: GranjaDto? = null,
    val receptor_titular: TitularDto? = null,
    val terra_desti: DanPreparationTerraDto? = null,
    val dan: DanRefDto? = null
)
