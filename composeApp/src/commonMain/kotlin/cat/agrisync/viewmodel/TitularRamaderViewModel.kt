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
                _uiState.update {
                    it.copy(isLoading = false, titular = titular, granges = granges, granjaBestiar = gb, entregues = entregues)
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = mapHttpError(ex.message)) }
            }
        }
    }

    fun updateTitular(nif: String, nom: String) {
        scope.launch {
            try {
                val updated = repository.updateTitular(currentTitularId, TitularUpdateRequest(nif = nif, nom_rao = nom))
                _uiState.update { it.copy(titular = updated, saveMessage = "Titular guardat") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun updateGranja(granjaId: String, nom: String, marca: String) {
        scope.launch {
            try {
                val updated = repository.updateGranja(granjaId, GranjaUpdateRequest(nom = nom, marca_oficial = marca))
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
    }

    fun updateGranjaBestiar(id: String, cens: Double) {
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
    }

    fun updateEntrega(id: String, data: String, quantitat: Double) {
        scope.launch {
            try {
                val result = repository.updateEntrega(id, EntregaUpdateRequest(data = data, quantitat = quantitat))
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
