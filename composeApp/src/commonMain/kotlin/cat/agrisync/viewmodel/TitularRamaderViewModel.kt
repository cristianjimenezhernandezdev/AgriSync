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

data class TitularRamaderUiState(
    val titular: TitularDto? = null,
    val granges: List<GranjaDto> = emptyList(),
    val granjaBestiar: List<GranjaBestiarDto> = emptyList(),
    val entregues: List<EntregaDejeccioDto> = emptyList(),
    val terres: List<TerraDto> = emptyList(),
    val bestiars: List<BestiarDto> = emptyList(),
    val fasesProductives: List<FaseProductivaDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
)

internal class TitularRamaderViewModel(
    private val repository: RamaderRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularRamaderUiState())
    val uiState: StateFlow<TitularRamaderUiState> = _uiState.asStateFlow()
    private var currentTitularId: String = ""

    fun load(titularId: String) {
        currentTitularId = titularId
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, saveMessage = null) }
            try {
                val titular = repository.getTitular(titularId)
                val granges = repository.listGranges(titularId)
                val gb = repository.listGranjaBestiar(titularId)
                val entregues = repository.listEntreguesByTitular(titularId)
                val terres = repository.listTerres(titularId)
                val bestiars = repository.listBestiarCatalog()
                val fases = repository.listFaseProductivaCatalog()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        granges = granges,
                        granjaBestiar = gb,
                        entregues = entregues,
                        terres = terres,
                        bestiars = bestiars,
                        fasesProductives = fases
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
                _uiState.update { it.copy(titular = updated, saveMessage = "Titular guardat") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun updateGranja(granjaId: String, nom: String, marca: String): Boolean {
        if (marca.isBlank()) {
            _uiState.update { it.copy(saveMessage = "La marca oficial es obligatoria") }
            return false
        }
        scope.launch {
            try {
                val updated = repository.updateGranja(
                    granjaId,
                    GranjaUpdateRequest(nom = nom.trim().ifBlank { null }, marca_oficial = marca.trim())
                )
                _uiState.update { st ->
                    st.copy(
                        granges = st.granges.map { if (it.id == granjaId) updated else it },
                        saveMessage = "Granja guardada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createGranja(nom: String, marca: String): Boolean {
        val cleanMarca = marca.trim()
        val cleanNom = nom.trim()
        if (cleanMarca.isBlank()) {
            _uiState.update { it.copy(saveMessage = "La marca oficial es obligatoria") }
            return false
        }
        scope.launch {
            try {
                val created = repository.createGranja(
                    GranjaCreateRequest(
                        titular_id = currentTitularId,
                        marca_oficial = cleanMarca,
                        nom = cleanNom.ifBlank { null }
                    )
                )
                _uiState.update { st ->
                    st.copy(
                        granges = listOf(created) + st.granges,
                        saveMessage = "Granja creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteGranja(granjaId: String) {
        scope.launch {
            try {
                repository.deleteGranja(granjaId)
                _uiState.update { st ->
                    st.copy(
                        granges = st.granges.filterNot { it.id == granjaId },
                        granjaBestiar = st.granjaBestiar.filterNot { it.granja?.id == granjaId },
                        entregues = st.entregues.filterNot { it.granja_origen_id == granjaId },
                        saveMessage = "Granja eliminada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun updateGranjaBestiar(id: String, censText: String): Boolean {
        val cens = censText.toDoubleOrNull()
        if (cens == null) {
            _uiState.update { it.copy(saveMessage = "El cens ha de ser un nombre valid") }
            return false
        }
        if (cens < 0) {
            _uiState.update { it.copy(saveMessage = "El cens no pot ser negatiu") }
            return false
        }
        scope.launch {
            try {
                val result = repository.updateGranjaBestiar(id, GranjaBestiarUpdateRequest(cens = cens))
                if (result.isNotEmpty()) {
                    val updated = result.first()
                    _uiState.update { st ->
                        st.copy(
                            granjaBestiar = st.granjaBestiar.map { if (it.id == id) updated else it },
                            saveMessage = "Bestiar guardat"
                        )
                    }
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createGranjaBestiar(granjaId: String, bestiarId: String, faseId: String, censText: String): Boolean {
        val cens = censText.toDoubleOrNull()
        if (granjaId.isBlank() || bestiarId.isBlank() || faseId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar granja, bestiar i fase") }
            return false
        }
        if (cens == null) {
            _uiState.update { it.copy(saveMessage = "El cens ha de ser un nombre valid") }
            return false
        }
        if (cens < 0) {
            _uiState.update { it.copy(saveMessage = "El cens no pot ser negatiu") }
            return false
        }
        scope.launch {
            try {
                val created = repository.createGranjaBestiar(
                    GranjaBestiarCreateRequest(
                        granja_id = granjaId,
                        bestiar_id = bestiarId,
                        fase_productiva_id = faseId,
                        cens = cens
                    )
                )
                _uiState.update { st ->
                    st.copy(
                        granjaBestiar = listOf(created) + st.granjaBestiar,
                        saveMessage = "Registre de bestiar creat correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteGranjaBestiar(id: String) {
        scope.launch {
            try {
                repository.deleteGranjaBestiar(id)
                _uiState.update { st ->
                    st.copy(
                        granjaBestiar = st.granjaBestiar.filterNot { it.id == id },
                        saveMessage = "Registre de bestiar eliminat"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun updateEntrega(id: String, data: String, quantitatText: String): Boolean {
        val cleanData = data.trim()
        val quantitat = quantitatText.toDoubleOrNull()
        if (!isValidIsoDate(cleanData)) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format YYYY-MM-DD") }
            return false
        }
        if (quantitat == null) {
            _uiState.update { it.copy(saveMessage = "La quantitat ha de ser un nombre valid") }
            return false
        }
        if (quantitat < 0) {
            _uiState.update { it.copy(saveMessage = "La quantitat no pot ser negativa") }
            return false
        }
        scope.launch {
            try {
                val result = repository.updateEntrega(id, EntregaUpdateRequest(data = cleanData, quantitat = quantitat))
                if (result.isNotEmpty()) {
                    val updated = result.first()
                    _uiState.update { st ->
                        st.copy(
                            entregues = st.entregues.map { if (it.id == id) updated else it },
                            saveMessage = "Entrega guardada"
                        )
                    }
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun createEntrega(
        granjaOrigenId: String,
        data: String,
        quantitatText: String,
        terraDestiId: String?,
        receptorTitularId: String?
    ): Boolean {
        val cleanData = data.trim()
        val quantitat = quantitatText.toDoubleOrNull()
        if (granjaOrigenId.isBlank()) {
            _uiState.update { it.copy(saveMessage = "Has de seleccionar una granja d'origen") }
            return false
        }
        if (!isValidIsoDate(cleanData)) {
            _uiState.update { it.copy(saveMessage = "La data ha de tenir format YYYY-MM-DD") }
            return false
        }
        if (quantitat == null) {
            _uiState.update { it.copy(saveMessage = "La quantitat ha de ser un nombre valid") }
            return false
        }
        if (quantitat < 0) {
            _uiState.update { it.copy(saveMessage = "La quantitat no pot ser negativa") }
            return false
        }
        val hasTerra = !terraDestiId.isNullOrBlank()
        val hasTitular = !receptorTitularId.isNullOrBlank()
        if (hasTerra == hasTitular) {
            _uiState.update { it.copy(saveMessage = "Has d'escollir exactament un tipus de receptor") }
            return false
        }

        scope.launch {
            try {
                val created = repository.createEntrega(
                    titularId = currentTitularId,
                    granjaOrigenId = granjaOrigenId,
                    data = cleanData,
                    quantitat = quantitat,
                    terraDestiId = terraDestiId,
                    receptorTitularId = receptorTitularId
                )
                _uiState.update { st ->
                    st.copy(
                        entregues = listOf(created) + st.entregues,
                        saveMessage = "Entrega creada correctament"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
        return true
    }

    fun deleteEntrega(id: String) {
        scope.launch {
            try {
                repository.deleteEntrega(id)
                _uiState.update { st ->
                    st.copy(
                        entregues = st.entregues.filterNot { it.id == id },
                        saveMessage = "Entrega eliminada"
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

    private fun mapHttpError(message: String?): String {
        val msg = message ?: return "Error desconegut"
        return when {
            msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
            msg.contains("403") -> "No tens permis per aquest titular (403)."
            else -> msg
        }
    }
}

private fun isValidIsoDate(value: String): Boolean {
    return value.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))
}
