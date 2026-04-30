package cat.agrisync.data

internal object SchemaCompatibility {
    const val legacyAplicacioEntregaField = "entrega_id"
    const val legacyEntregaTipusField = "tipus_fertilitzant"
    const val legacyEntregaVolumField = "volum_m3"
    const val optionalGranjaCampanyaBalanceTable = "granja_campanya_balance"

    const val agricolaAplicacioSelect =
        "?select=id,terra_id,data,tipus_fertilitzant,procedencia,volum_m3,kg_n_m3,kg_n,tecnic_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya)"

    const val danPreparationAplicacioSelect =
        "?select=id,data,tipus_fertilitzant,procedencia,volum_m3,kg_n_m3,kg_n,terra:terra_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha),dan:dan_id(id,titular_id,campanya)"

    const val danPreparationEntregaSelect =
        "?select=id,data,volum_m3,kg_n_m3,kg_n,granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom),terra_desti:terra_desti_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha),dan:dan_id(id,titular_id,campanya)"

    /** Fallback per a BDD sense columnes de volum/nitrogen a entrega_dejeccions */
    const val danPreparationEntregaSelectNoVolum =
        "?select=id,data,granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom),terra_desti:terra_desti_id(id,titular_id,mun_codi,poligon,parcela,recinte,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha),dan:dan_id(id,titular_id,campanya)"

    const val ramaderEntregaSelect =
        "?select=id,data,volum_m3,kg_n_m3,kg_n,granja_origen_id,terra_desti_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya),granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom,updated_at,updated_by),terra_desti:terra_desti_id(id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,titular:titular_id(id,nif,nom_rao,telefon,email,adreca,codi_postal))"

    /** Fallback per a BDD sense columnes de volum/nitrogen a entrega_dejeccions */
    const val ramaderEntregaSelectNoVolum =
        "?select=id,data,granja_origen_id,terra_desti_id,updated_at,updated_by,dan:dan_id(id,titular_id,campanya),granja_origen:granja_origen_id(id,titular_id,marca_oficial,nom,updated_at,updated_by),terra_desti:terra_desti_id(id,titular_id,codi_sigpac_complet,municipi_literal,us_sigpac,cultiu,superficie,zona,limit_kg_n_ha,titular:titular_id(id,nif,nom_rao,telefon,email,adreca,codi_postal))"

    fun isMissingSchemaCacheTable(message: String?, table: String): Boolean {
        val msg = message ?: return false
        return msg.contains("PGRST205") && msg.contains(table)
    }

    fun isMissingColumn(message: String?, column: String, table: String? = null): Boolean {
        val msg = message ?: return false
        if (!msg.contains("42703") || !msg.contains(column)) return false
        return table == null || msg.contains(table)
    }

    fun isMissingRelationship(message: String?): Boolean {
        val msg = message ?: return false
        return msg.contains("PGRST200") || msg.contains("Could not find a relationship between")
    }
}
