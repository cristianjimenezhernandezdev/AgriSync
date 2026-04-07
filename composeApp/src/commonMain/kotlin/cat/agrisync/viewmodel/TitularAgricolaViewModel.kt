package cat.agrisync.viewmodel

import cat.agrisync.data.*
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
    val terres: List<TerraDto> = emptyList(),
    val aplicacions: List<AplicacioFertilitzantDto> = emptyList(),
    val actorLabels: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
)

internal class TitularAgricolaViewModel(
    private val repository: AgricolaRepository,
    private val auditRepository: AuditRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularAgricolaUiState())
    val uiState: StateFlow<TitularAgricolaUiState> = _uiState.asStateFlow()
    private var currentTitularId: String = ""

    fun load(titularId: String) {
        currentTitularId = titularId
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveMessage = null) }
            try {
                val titular = repository.getTitular(titularId)
                val terres = repository.listTerres(titularId)
                val aplicacions = repository.listAplicacionsByTitular(titularId)
                val actorLabels = resolveActorLabels(titular, terres, aplicacions)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        terres = terres,
                        aplicacions = aplicacions,
                        actorLabels = actorLabels
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapHttpError(ex.message)) }
            }
        }
    }

    fun updateTitular(nif: String, nom: String): Boolean {
        if (nom.isBlank()) {
            _uiState.update { it.copy(saveMessage = "El nom del titular es obligatori") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateTitular(
                    currentTitularId,
                    TitularUpdateRequest(
                        nif = nif.trim().ifBlank { null },
                        nom_rao = nom.trim()
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

    fun updateTerra(terraId: String, superficieText: String): Boolean {
        val superficie = superficieText.toDoubleOrNull()
        if (superficie == null) {
            _uiState.update { it.copy(saveMessage = "La superficie ha de ser un nombre valid") }
            return false
        }
        if (superficie < 0) {
            _uiState.update { it.copy(saveMessage = "La superficie no pot ser negativa") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateTerra(terraId, TerraUpdateRequest(superficie = superficie))
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
        superficieText: String
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
                        superficie = superficie
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

    fun updateAplicacio(id: String, data: String, kgNText: String, ufText: String): Boolean {
        val cleanData = data.trim()
        val kgN = kgNText.toDoubleOrNull()
        val uf = ufText.toDoubleOrNull()
        if (!isValidIsoDate(cleanData)) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format YYYY-MM-DD") }
            return false
        }
        if (kgN == null || uf == null) {
            _uiState.update { it.copy(saveMessage = "Kg N i UF han de ser nombres valids") }
            return false
        }
        if (kgN < 0 || uf < 0) {
            _uiState.update { it.copy(saveMessage = "Kg N i UF no poden ser negatius") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateAplicacio(
                    id,
                    AplicacioUpdateRequest(data = cleanData, kg_n = kgN, uf = uf)
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, updated.updated_by)
                _uiState.update { st ->
                    st.copy(
                        aplicacions = st.aplicacions.map { if (it.id == id) updated else it },
                        actorLabels = actorLabels,
                        saveMessage = "Aplicacio guardada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createAplicacio(terraId: String, data: String, kgNText: String, ufText: String): Boolean {
        val cleanData = data.trim()
        val kgN = kgNText.toDoubleOrNull()
        val uf = ufText.toDoubleOrNull()
        if (terraId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una terra") }
            return false
        }
        if (!isValidIsoDate(cleanData)) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format YYYY-MM-DD") }
            return false
        }
        if (kgN == null || uf == null) {
            _uiState.update { it.copy(saveMessage = "Kg N i UF han de ser nombres valids") }
            return false
        }
        if (kgN < 0 || uf < 0) {
            _uiState.update { it.copy(saveMessage = "Kg N i UF no poden ser negatius") }
            return false
        }
        scope.launch {
            try {
                val created = repository.createAplicacio(
                    titularId = currentTitularId,
                    terraId = terraId,
                    data = cleanData,
                    kgN = kgN,
                    uf = uf
                )
                val actorLabels = updateActorLabels(_uiState.value.actorLabels, created.updated_by)
                _uiState.update { st ->
                    st.copy(
                        aplicacions = listOf(created) + st.aplicacions,
                        actorLabels = actorLabels,
                        saveMessage = "Aplicacio creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteAplicacio(id: String) {
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
}

private fun mapHttpError(message: String?): String {
    val msg = message ?: return "Error desconegut"
    return when {
        msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
        msg.contains("403") -> "No tens permis per aquest titular (403)."
        else -> msg
    }
}

private fun isValidIsoDate(value: String): Boolean {
    return value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
}
