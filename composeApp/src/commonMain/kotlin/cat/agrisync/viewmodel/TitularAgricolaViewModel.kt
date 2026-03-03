package cat.agrisync.viewmodel

import cat.agrisync.data.AgricolaRepository
import cat.agrisync.data.AplicacioFertilitzantDto
import cat.agrisync.data.TerraDto
import cat.agrisync.data.TitularDto
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
    val error: String? = null
)

internal class TitularAgricolaViewModel(
    private val repository: AgricolaRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularAgricolaUiState())
    val uiState: StateFlow<TitularAgricolaUiState> = _uiState.asStateFlow()

    fun load(titularId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val titular = repository.getTitular(titularId)
                val terres = repository.listTerres(titularId)
                val aplicacions = repository.listAplicacionsByTitular(titularId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        terres = terres,
                        aplicacions = aplicacions
                    )
                }
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = mapHttpError(ex.message)
                    )
                }
            }
        }
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

