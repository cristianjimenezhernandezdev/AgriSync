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
    val isLoading: Boolean = false,
    val error: String? = null,
    val saveMessage: String? = null
)

internal class TitularAgricolaViewModel(
    private val repository: AgricolaRepository
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
                _uiState.update {
                    it.copy(isLoading = false, titular = titular, terres = terres, aplicacions = aplicacions)
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

    fun updateTerra(terraId: String, superficie: Double) {
        scope.launch {
            try {
                val updated = repository.updateTerra(terraId, TerraUpdateRequest(superficie = superficie))
                _uiState.update { st ->
                    st.copy(
                        terres = st.terres.map { if (it.id == terraId) updated else it },
                        saveMessage = "Terra guardada"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(saveMessage = "Error: ${ex.message}") }
            }
        }
    }

    fun updateAplicacio(id: String, data: String, kgN: Double, uf: Double) {
        scope.launch {
            try {
                val updated = repository.updateAplicacio(id, AplicacioUpdateRequest(data = data, kg_n = kgN, uf = uf))
                _uiState.update { st ->
                    st.copy(
                        aplicacions = st.aplicacions.map { if (it.id == id) updated else it },
                        saveMessage = "Aplicacio guardada"
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
}

private fun mapHttpError(message: String?): String {
    val msg = message ?: return "Error desconegut"
    return when {
        msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
        msg.contains("403") -> "No tens permis per aquest titular (403)."
        else -> msg
    }
}
