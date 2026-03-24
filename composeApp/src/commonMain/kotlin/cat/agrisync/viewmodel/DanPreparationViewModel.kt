package cat.agrisync.viewmodel

import cat.agrisync.data.DanPreparationAplicacioDto
import cat.agrisync.data.DanPreparationEntregaDto
import cat.agrisync.data.DanPreparationRepository
import cat.agrisync.data.DanPreparationTerraDto
import cat.agrisync.data.GranjaBestiarDto
import cat.agrisync.data.GranjaDto
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

internal data class DanPreparationUiState(
    val titular: TitularDto? = null,
    val terres: List<DanPreparationTerraDto> = emptyList(),
    val aplicacions: List<DanPreparationAplicacioDto> = emptyList(),
    val granges: List<GranjaDto> = emptyList(),
    val granjaBestiar: List<GranjaBestiarDto> = emptyList(),
    val entregues: List<DanPreparationEntregaDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalHectares: Double
        get() = terres.sumOf { it.superficie ?: 0.0 }

    val totalKgN: Double
        get() = aplicacions.sumOf { it.kg_n ?: 0.0 }

    val totalUf: Double
        get() = aplicacions.sumOf { it.uf ?: 0.0 }

    val totalCens: Double
        get() = granjaBestiar.sumOf { it.cens ?: 0.0 }

    val totalQuantitatEntregada: Double
        get() = entregues.sumOf { it.quantitat ?: 0.0 }

    val kgNPerHa: Double?
        get() = totalHectares.takeIf { it > 0 }?.let { totalKgN / it }

    val kgNPerUf: Double?
        get() = totalUf.takeIf { it > 0 }?.let { totalKgN / it }

    val campanyes: List<Int>
        get() = (aplicacions.mapNotNull { it.dan?.campanya } + entregues.mapNotNull { it.dan?.campanya })
            .distinct()
            .sortedDescending()
}

internal class DanPreparationViewModel(
    private val repository: DanPreparationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(DanPreparationUiState())
    val uiState: StateFlow<DanPreparationUiState> = _uiState.asStateFlow()

    fun load(titularId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val titular = repository.getTitular(titularId)
                val terres = repository.listTerres(titularId)
                val aplicacions = repository.listAplicacionsByTitular(titularId)
                val granges = repository.listGranges(titularId)
                val granjaBestiar = repository.listGranjaBestiar(titularId)
                val entregues = repository.listEntreguesByTitular(titularId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        titular = titular,
                        terres = terres,
                        aplicacions = aplicacions,
                        granges = granges,
                        granjaBestiar = granjaBestiar,
                        entregues = entregues
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ex.message ?: "Error carregant el resum DAN") }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
