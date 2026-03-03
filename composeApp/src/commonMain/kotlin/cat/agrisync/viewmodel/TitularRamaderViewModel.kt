package cat.agrisync.viewmodel

import cat.agrisync.data.EntregaDejeccioDto
import cat.agrisync.data.GranjaBestiarDto
import cat.agrisync.data.GranjaDto
import cat.agrisync.data.RamaderRepository
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

data class TitularRamaderUiState(
    val titular: TitularDto? = null,
    val granges: List<GranjaDto> = emptyList(),
    val granjaBestiar: List<GranjaBestiarDto> = emptyList(),
    val entregues: List<EntregaDejeccioDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

internal class TitularRamaderViewModel(
    private val repository: RamaderRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularRamaderUiState())
    val uiState: StateFlow<TitularRamaderUiState> = _uiState.asStateFlow()

    fun load(titularId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val titular = repository.getTitular(titularId)
                val granges = repository.listGranges(titularId)
                val gb = repository.listGranjaBestiar(titularId)
                val entregues = repository.listEntreguesByTitular(titularId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        granges = granges,
                        granjaBestiar = gb,
                        entregues = entregues
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

    private fun mapHttpError(message: String?): String {
        val msg = message ?: return "Error desconegut"
        return when {
            msg.contains("401") -> "Sessio caducada (401). Torna a iniciar sessio."
            msg.contains("403") -> "No tens permis per aquest titular (403)."
            else -> msg
        }
    }
}
