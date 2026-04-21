package cat.agrisync.data

import kotlinx.serialization.Serializable

internal class AccessRepository(private val restClient: RestClient) {

    /**
     * Per admin/oficina_manager: retorna tots els titulars amb can_agricola=true, can_ramader=true.
     * Per tècnic normal: filtra segons les assignacions a tecnic_titular.
     * Usa el token de l'usuari autenticat — les RLS policies controlen l'accés.
     */
    internal suspend fun listTitularAccessForTecnic(tecnic: TecnicDto): List<TitularAccessRow> {
        val isAdmin = tecnic.rol == "admin"

        if (isAdmin) {
            // Admin/Manager veu tots els titulars
            val titulars: List<TitularRawDto> = restClient.get(
                "titular",
                "?select=id,nif,nom_rao,updated_at,updated_by&order=nom_rao"
            )
            return titulars.map { t ->
                TitularAccessRow(
                    titular_id = t.id,
                    nom = t.nom_rao,
                    nif = t.nif,
                    can_agricola = true,
                    can_ramader = true,
                    last_update_at = t.updated_at,
                    last_update_by = t.updated_by
                )
            }
        } else {
            val isManager = tecnic.rol == "oficina_manager"
            if (isManager) {
                val titulars: List<TitularRawDto> = restClient.get(
                    "titular",
                    "?select=id,nif,nom_rao,updated_at,updated_by&order=nom_rao"
                )
                val sharedRows: List<OficinaTitularShareAccessDto> = restClient.get(
                    "oficina_titular_compartit",
                    "?select=titular_id,scope&oficina_id=eq.${tecnic.oficina_id}"
                )
                val sharedByTitular = sharedRows.groupBy { it.titular_id }

                return titulars.map { titular ->
                    val scopes = sharedByTitular[titular.id].orEmpty().map { it.scope }
                    val isShared = scopes.isNotEmpty()
                    val canAgricola = if (isShared) scopes.any { it == "comu" || it == "agricola" } else true
                    val canRamader = if (isShared) scopes.any { it == "comu" || it == "ramader" } else true
                    TitularAccessRow(
                        titular_id = titular.id,
                        nom = titular.nom_rao,
                        nif = titular.nif,
                        can_agricola = canAgricola,
                        can_ramader = canRamader,
                        last_update_at = titular.updated_at,
                        last_update_by = titular.updated_by
                    )
                }
            }

            // Tècnic normal: consulta les assignacions
            val assignacions: List<TecnicTitularAccessDto> = restClient.get(
                "tecnic_titular",
                "?select=titular_id,scope,actiu,titular:titular_id(id,nif,nom_rao,updated_at,updated_by)&tecnic_id=eq.${tecnic.id}&actiu=eq.true"
            )

            // Agrupa per titular
            val grouped = assignacions.groupBy { it.titular_id }
            return grouped.mapNotNull { (titularId, entries) ->
                val titular = entries.firstOrNull()?.titular ?: return@mapNotNull null
                val scopes = entries.map { it.scope }
                val canAgricola = scopes.any { it == "comu" || it == "agricola" }
                val canRamader = scopes.any { it == "comu" || it == "ramader" }
                if (!canAgricola && !canRamader) return@mapNotNull null
                TitularAccessRow(
                    titular_id = titularId,
                    nom = titular.nom_rao,
                    nif = titular.nif,
                    can_agricola = canAgricola,
                    can_ramader = canRamader,
                    last_update_at = titular.updated_at,
                    last_update_by = titular.updated_by
                )
            }.sortedBy { it.nom }
        }
    }
}

@Serializable
internal data class TitularRawDto(
    val id: String,
    val nif: String? = null,
    val nom_rao: String,
    val updated_at: String? = null,
    val updated_by: String? = null
)

@Serializable
internal data class TecnicTitularAccessDto(
    val titular_id: String,
    val scope: String,
    val actiu: Boolean = true,
    val titular: TitularRawDto? = null
)

@Serializable
internal data class OficinaTitularShareAccessDto(
    val titular_id: String,
    val scope: String
)
