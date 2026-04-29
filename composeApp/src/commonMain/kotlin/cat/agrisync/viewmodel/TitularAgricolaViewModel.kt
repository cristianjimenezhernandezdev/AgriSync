package cat.agrisync.viewmodel

import cat.agrisync.data.*
import cat.agrisync.util.parseEnteredDateToIso
import cat.agrisync.util.validateAndResolveNitrogenTriplet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TitularAgricolaUiState(
    val titular: TitularDto? = null,
    val collaboratingTecnics: List<TitularCollaboratingTecnicSummary> = emptyList(),
    val collaboratingOficines: List<TitularCollaboratingOficinaSummary> = emptyList(),
    val terres: List<TerraDto> = emptyList(),
    val aplicacions: List<AplicacioFertilitzantDto> = emptyList(),
    val availableCampanyes: List<Int> = emptyList(),
    val selectedCampanya: Int = 0,
    val actorLabels: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
) {
    fun appliedKgNForTerra(terraId: String): Double {
        return aplicacions
            .filter { it.terra_id == terraId && it.dan?.campanya == selectedCampanya }
            .sumOf { it.kg_n ?: 0.0 }
    }
}

internal class TitularAgricolaViewModel(
    private val repository: AgricolaRepository,
    private val auditRepository: AuditRepository,
    private val tecnicRepository: TecnicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularAgricolaUiState())
    val uiState: StateFlow<TitularAgricolaUiState> = _uiState.asStateFlow()
    private var currentTitularId: String = ""

    fun load(titularId: String, preferredCampanya: Int? = null) {
        currentTitularId = titularId
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveMessage = null) }
            try {
                val titular = repository.getTitular(titularId)
                val collaboratingTecnicsRaw = tecnicRepository.listCollaboratingTecnicsByTitular(titularId)
                val collaboratingOficinesRaw = tecnicRepository.listCollaboratingOficinesByTitular(titularId)
                val terres = repository.listTerres(titularId)
                val existingCampanyes = repository.listCampanyesByTitular(titularId)
                val selectedCampanya = resolveSelectedCampanya(existingCampanyes, preferredCampanya)
                val aplicacions = repository.listAplicacionsByTitular(titularId, selectedCampanya)
                val actorLabels = resolveActorLabels(titular, terres, aplicacions)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        collaboratingTecnics = collaboratingTecnicsRaw.toTitularCollaboratingTecnicSummaries(),
                        collaboratingOficines = buildTitularCollaboratingOficinaSummaries(
                            collaboratingTecnicsRaw,
                            collaboratingOficinesRaw
                        ),
                        terres = terres,
                        aplicacions = aplicacions,
                        availableCampanyes = normalizedCampanyes(existingCampanyes),
                        selectedCampanya = selectedCampanya,
                        actorLabels = actorLabels
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapHttpError(ex.message)) }
            }
        }
    }

    fun onSelectCampanya(campanya: Int) {
        if (currentTitularId.isBlank()) return
        load(currentTitularId, campanya)
    }

    fun updateTitular(nif: String, nom: String, telefon: String, email: String, adreca: String, codiPostal: String): Boolean {
        if (nom.isBlank()) {
            _uiState.update { it.copy(saveMessage = "El nom del titular es obligatori") }
            return false
        }
        val cleanCodiPostal = codiPostal.trim()
        if (cleanCodiPostal.isNotBlank() && !cleanCodiPostal.matches(Regex("^\\d{5}$"))) {
            _uiState.update { it.copy(saveMessage = "El codi postal ha de tenir 5 digits") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateTitular(
                    currentTitularId,
                    TitularUpdateRequest(
                        nif = nif.trim().ifBlank { null },
                        nom_rao = nom.trim(),
                        telefon = telefon.trim().ifBlank { null },
                        email = email.trim().ifBlank { null },
                        adreca = adreca.trim().ifBlank { null },
                        codi_postal = cleanCodiPostal.ifBlank { null }
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                _uiState.update { it.copy(titular = updated, actorLabels = actorLabels, saveMessage = "Titular guardat") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun updateTerra(
        terraId: String,
        superficieText: String,
        zona: String,
        municipiLiteral: String,
        usSigpac: String,
        cultiu: String
    ): Boolean {
        val superficie = superficieText.toDoubleOrNull()
        if (superficie == null) {
            _uiState.update { it.copy(saveMessage = "La superficie ha de ser un nombre valid") }
            return false
        }
        if (superficie < 0) {
            _uiState.update { it.copy(saveMessage = "La superficie no pot ser negativa") }
            return false
        }
        if (zona != "ZV" && zona != "ZNV") {
            _uiState.update { it.copy(saveMessage = "La zona ha de ser ZV o ZNV") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateTerra(
                    terraId,
                    TerraUpdateRequest(
                        municipi_literal = municipiLiteral.trim().ifBlank { null },
                        us_sigpac = usSigpac.trim().ifBlank { null },
                        cultiu = cultiu.trim().ifBlank { null },
                        superficie = superficie,
                        zona = zona
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                _uiState.update { st ->
                    st.copy(
                        terres = st.terres.map { if (it.id == terraId) updated else it },
                        actorLabels = actorLabels,
                        saveMessage = "Terra guardada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createTerra(
        munCodi: String,
        poligonText: String,
        parcelaText: String,
        recinteText: String,
        superficieText: String,
        zona: String,
        municipiLiteral: String,
        usSigpac: String,
        cultiu: String
    ): Boolean {
        val cleanMunCodi = munCodi.trim()
        val poligon = poligonText.toIntOrNull()
        val parcela = parcelaText.toIntOrNull()
        val recinte = recinteText.toIntOrNull()
        val superficie = superficieText.toDoubleOrNull()

        when {
            !cleanMunCodi.matches(Regex("^\\d{5}$")) -> {
                _uiState.update { it.copy(saveMessage = "El codi municipal ha de tenir 5 digits") }
                return false
            }
            poligon == null || poligon <= 0 -> {
                _uiState.update { it.copy(saveMessage = "El poligon ha de ser un enter positiu") }
                return false
            }
            parcela == null || parcela <= 0 -> {
                _uiState.update { it.copy(saveMessage = "La parcela ha de ser un enter positiu") }
                return false
            }
            recinte == null || recinte <= 0 -> {
                _uiState.update { it.copy(saveMessage = "El recinte ha de ser un enter positiu") }
                return false
            }
            superficie == null -> {
                _uiState.update { it.copy(saveMessage = "La superficie ha de ser un nombre valid") }
                return false
            }
            superficie < 0 -> {
                _uiState.update { it.copy(saveMessage = "La superficie no pot ser negativa") }
                return false
            }
            zona != "ZV" && zona != "ZNV" -> {
                _uiState.update { it.copy(saveMessage = "La zona ha de ser ZV o ZNV") }
                return false
            }
        }

        scope.launch {
            try {
                val created = repository.createTerra(
                    TerraCreateRequest(
                        titular_id = currentTitularId,
                        mun_codi = cleanMunCodi,
                        poligon = poligon,
                        parcela = parcela,
                        recinte = recinte,
                        municipi_literal = municipiLiteral.trim().ifBlank { null },
                        us_sigpac = usSigpac.trim().ifBlank { null },
                        cultiu = cultiu.trim().ifBlank { null },
                        superficie = superficie,
                        zona = zona
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, created.updated_by)
                _uiState.update { st ->
                    st.copy(
                        terres = listOf(created) + st.terres,
                        actorLabels = actorLabels,
                        saveMessage = "Terra creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteTerra(terraId: String) {
        scope.launch {
            try {
                repository.deleteTerra(terraId)
                _uiState.update { st ->
                    st.copy(
                        terres = st.terres.filterNot { it.id == terraId },
                        saveMessage = "Terra eliminada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun updateAplicacio(
        id: String,
        data: String,
        kgNText: String,
        tipusFertilitzant: String,
        procedencia: String,
        volumText: String,
        kgNM3Text: String
    ): Boolean {
        val currentApp = _uiState.value.aplicacions.firstOrNull { it.id == id }
        if (currentApp?.isSynchronizedFromRamader() == true) {
            _uiState.update { it.copy(saveMessage = "Aquesta aplicacio ve d'una entrega ramadera. Edita-la des del modul ramader.") }
            return false
        }
        val cleanData = parseEnteredDateToIso(data)
        if (cleanData == null) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format dd/MM/YYYY") }
            return false
        }
        val nutrients = validateAndResolveNitrogenTriplet(
            kgNText = kgNText,
            volumM3Text = volumText,
            kgNPerM3Text = kgNM3Text
        )
        if (nutrients.errorMessage != null) {
            _uiState.update { it.copy(saveMessage = nutrients.errorMessage) }
            return false
        }
        val resolved = nutrients.values ?: return false
        scope.launch {
            try {
                val updated = repository.updateAplicacio(
                    id,
                    AplicacioUpdateRequest(
                        data = cleanData,
                        tipus_fertilitzant = tipusFertilitzant.trim().ifBlank { null },
                        procedencia = procedencia.trim().ifBlank { null },
                        volum_m3 = resolved.volumM3,
                        kg_n_m3 = resolved.kgNPerM3,
                        kg_n = resolved.kgN
                    )
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                _uiState.update { st ->
                    val updatedAplicacions = st.aplicacions.map { if (it.id == id) updated else it }
                    val warning = buildNitrogenLimitWarning(updated.terra_id ?: "", updatedAplicacions, st.selectedCampanya)
                    st.copy(
                        aplicacions = updatedAplicacions,
                        actorLabels = actorLabels,
                        saveMessage = warning ?: "Aplicacio guardada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createAplicacio(
        terraId: String,
        data: String,
        kgNText: String,
        tipusFertilitzant: String,
        procedencia: String,
        volumText: String,
        kgNM3Text: String
    ): Boolean {
        val cleanData = parseEnteredDateToIso(data)
        if (terraId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una terra") }
            return false
        }
        if (cleanData == null) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format dd/MM/YYYY") }
            return false
        }
        val nutrients = validateAndResolveNitrogenTriplet(
            kgNText = kgNText,
            volumM3Text = volumText,
            kgNPerM3Text = kgNM3Text
        )
        if (nutrients.errorMessage != null) {
            _uiState.update { it.copy(saveMessage = nutrients.errorMessage) }
            return false
        }
        val resolved = nutrients.values ?: return false
        scope.launch {
            try {
                val created = repository.createAplicacio(
                    titularId = currentTitularId,
                    campanya = _uiState.value.selectedCampanya,
                    terraId = terraId,
                    data = cleanData,
                    tipusFertilitzant = tipusFertilitzant.trim().ifBlank { null },
                    procedencia = procedencia.trim().ifBlank { null },
                    volumM3 = resolved.volumM3,
                    kgNM3 = resolved.kgNPerM3,
                    kgN = resolved.kgN
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, created.updated_by)
                _uiState.update { st ->
                    val warning = buildNitrogenLimitWarning(created.terra_id ?: terraId, listOf(created) + st.aplicacions, st.selectedCampanya)
                    st.copy(
                        aplicacions = listOf(created) + st.aplicacions,
                        actorLabels = actorLabels,
                        saveMessage = warning ?: "Aplicacio creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteAplicacio(id: String) {
        val app = _uiState.value.aplicacions.firstOrNull { it.id == id }
        if (app?.isSynchronizedFromRamader() == true) {
            _uiState.update { it.copy(saveMessage = "Aquesta aplicacio ve d'una entrega ramadera. Elimina-la des del modul ramader.") }
            return
        }
        scope.launch {
            try {
                repository.deleteAplicacio(id)
                _uiState.update { st ->
                    st.copy(
                        aplicacions = st.aplicacions.filterNot { it.id == id },
                        saveMessage = "Aplicacio eliminada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }

    fun clear() {
        scope.cancel()
    }

    private suspend fun resolveActorLabels(
        titular: TitularDto?,
        terres: List<TerraDto>,
        aplicacions: List<AplicacioFertilitzantDto>
    ): Map<String, String> {
        return auditRepository.resolveActorLabels(
            buildList {
                add(titular?.updated_by)
                terres.forEach { add(it.updated_by) }
                aplicacions.forEach { add(it.updated_by) }
            }
        )
    }

    private suspend fun updateActorLabels(
        current: Map<String, String>,
        userId: String?
    ): Map<String, String> {
        val cleanUserId = userId?.takeIf { it.isNotBlank() } ?: return current
        if (current.containsKey(cleanUserId)) return current
        val label = auditRepository.resolveActorLabel(cleanUserId) ?: return current
        return current + (cleanUserId to label)
    }

    private fun buildNitrogenLimitWarning(
        terraId: String,
        aplicacionsActuals: List<AplicacioFertilitzantDto>,
        selectedCampanya: Int
    ): String? {
        val terra = _uiState.value.terres.firstOrNull { it.id == terraId } ?: return null
        val superficie = terra.superficie ?: return null
        if (superficie <= 0.0) return null

        val limitKgNHa = terra.limit_kg_n_ha ?: if (terra.zona == "ZV") 170.0 else 190.0
        val limitTotal = superficie * limitKgNHa
        val appliedTotal = aplicacionsActuals
            .filter { it.terra_id == terraId && it.dan?.campanya == selectedCampanya }
            .sumOf { it.kg_n ?: 0.0 }
        val excess = appliedTotal - limitTotal

        return if (excess > 0.0001) {
            "Aplicacio guardada, pero la terra supera el limit anual per campanya en ${formatKgN(excess)} kg N."
        } else {
            null
        }
    }
}

private fun formatKgN(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100
    return rounded.toString().replace('.', ',')
}

private fun mapHttpError(message: String?): String {
    val msg = message ?: return "Error desconegut"
    return when {
        msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
        msg.contains("403") -> "No tens permis per aquest titular (403)."
        msg.contains("42703") && msg.contains("entrega_id") ->
            "La base de dades actual no te el camp antic d'enllac amb entregues. S'ha bloquejat aquesta consulta per evitar l'error."
        msg.contains("PGRST200") || msg.contains("Could not find a relationship between") ->
            "No s'ha pogut carregar una relacio de dades del modul agricola. Torna-ho a provar i, si persisteix, cal revisar la configuracio de Supabase."
        else -> msg
    }
}
