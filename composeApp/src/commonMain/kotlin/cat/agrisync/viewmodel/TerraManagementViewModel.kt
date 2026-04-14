package cat.agrisync.viewmodel

import cat.agrisync.data.TerraCreateRequest
import cat.agrisync.data.TerraFullDto
import cat.agrisync.data.TerraUpdateFullRequest
import cat.agrisync.data.TitularDto
import cat.agrisync.data.TitularManagementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerraManagementUiState(
    val terres: List<TerraFullDto> = emptyList(),
    val titulars: List<TitularDto> = emptyList(),
    val searchQuery: String = "",
    val filterTitularId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    // Crear
    val showCreateDialog: Boolean = false,
    val newTitularId: String = "",
    val newMunCodi: String = "",
    val newPoligon: String = "",
    val newParcela: String = "",
    val newRecinte: String = "",
    val newMunicipiLiteral: String = "",
    val newUsSigpac: String = "",
    val newCultiu: String = "",
    val newSuperficie: String = "",
    val newZona: String = "ZNV",
    val isCreating: Boolean = false,
    // Editar
    val editingTerra: TerraFullDto? = null,
    val editTitularId: String = "",
    val editMunicipiLiteral: String = "",
    val editUsSigpac: String = "",
    val editCultiu: String = "",
    val editSuperficie: String = "",
    val editZona: String = "ZNV",
    val isEditing: Boolean = false,
    // Paginació
    val currentPage: Int = 0,
    val pageSize: Int = 15
) {
    val filtered: List<TerraFullDto>
        get() {
            var list = terres
            if (!filterTitularId.isNullOrBlank()) {
                list = list.filter { it.titular_id == filterTitularId }
            }
            if (searchQuery.isBlank()) return list
            val q = searchQuery.uppercase()
            return list.filter {
                (it.codi_sigpac_complet ?: "").uppercase().contains(q) ||
                        (it.titular?.nom_rao ?: "").contains(searchQuery, ignoreCase = true) ||
                        (it.titular?.nif ?: "").uppercase().contains(q)
            }
        }

    val totalPages: Int
        get() = if (filtered.isEmpty()) 1 else ((filtered.size - 1) / pageSize) + 1

    val pageItems: List<TerraFullDto>
        get() {
            val start = currentPage * pageSize
            if (start >= filtered.size) return emptyList()
            return filtered.subList(start, minOf(filtered.size, start + pageSize))
        }
}

internal class TerraManagementViewModel(
    private val repository: TitularManagementRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TerraManagementUiState())
    val uiState: StateFlow<TerraManagementUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val terres = repository.listTerres()
                val titulars = repository.listAll()
                _uiState.update { it.copy(isLoading = false, terres = terres, titulars = titulars, currentPage = 0) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error carregant terres") }
            }
        }
    }

    fun onSearchChange(value: String) {
        _uiState.update { it.copy(searchQuery = value, currentPage = 0) }
    }

    fun onFilterTitular(titularId: String?) {
        _uiState.update { it.copy(filterTitularId = titularId, currentPage = 0) }
    }

    fun nextPage() {
        _uiState.update { s -> if (s.currentPage + 1 < s.totalPages) s.copy(currentPage = s.currentPage + 1) else s }
    }

    fun prevPage() {
        _uiState.update { s -> if (s.currentPage > 0) s.copy(currentPage = s.currentPage - 1) else s }
    }

    // ── Crear ──
    fun showCreateDialog() {
        _uiState.update {
            it.copy(
                showCreateDialog = true,
                newTitularId = "", newMunCodi = "", newPoligon = "",
                newParcela = "", newRecinte = "", newMunicipiLiteral = "",
                newUsSigpac = "", newCultiu = "", newSuperficie = "", newZona = "ZNV"
            )
        }
    }

    fun hideCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }

    fun onNewTitularId(v: String) { _uiState.update { it.copy(newTitularId = v) } }
    fun onNewMunCodi(v: String) { _uiState.update { it.copy(newMunCodi = v) } }
    fun onNewPoligon(v: String) { _uiState.update { it.copy(newPoligon = v) } }
    fun onNewParcela(v: String) { _uiState.update { it.copy(newParcela = v) } }
    fun onNewRecinte(v: String) { _uiState.update { it.copy(newRecinte = v) } }
    fun onNewMunicipiLiteral(v: String) { _uiState.update { it.copy(newMunicipiLiteral = v) } }
    fun onNewUsSigpac(v: String) { _uiState.update { it.copy(newUsSigpac = v) } }
    fun onNewCultiu(v: String) { _uiState.update { it.copy(newCultiu = v) } }
    fun onNewSuperficie(v: String) { _uiState.update { it.copy(newSuperficie = v) } }
    fun onNewZona(v: String) { _uiState.update { it.copy(newZona = v) } }

    fun createTerra() {
        val s = _uiState.value
        val poligon = s.newPoligon.toIntOrNull()
        val parcela = s.newParcela.toIntOrNull()
        val recinte = s.newRecinte.toIntOrNull()
        val superficie = s.newSuperficie.toDoubleOrNull()

        if (s.newMunCodi.isBlank() || poligon == null || parcela == null || recinte == null || superficie == null) {
            _uiState.update { it.copy(message = "Tots els camps SIGPAC i superficie son obligatoris") }
            return
        }
        if (!s.newMunCodi.matches(Regex("^[0-9]{5}$"))) {
            _uiState.update { it.copy(message = "El codi municipal ha de ser 5 digits (ex: 17071)") }
            return
        }
        if (s.newZona != "ZV" && s.newZona != "ZNV") {
            _uiState.update { it.copy(message = "La zona ha de ser ZV o ZNV") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                val body = TerraCreateRequest(
                    titular_id = s.newTitularId.ifBlank { null },
                    mun_codi = s.newMunCodi,
                    poligon = poligon,
                    parcela = parcela,
                    recinte = recinte,
                    municipi_literal = s.newMunicipiLiteral.ifBlank { null },
                    us_sigpac = s.newUsSigpac.ifBlank { null },
                    cultiu = s.newCultiu.ifBlank { null },
                    superficie = superficie,
                    zona = s.newZona
                )
                repository.createTerra(body)
                _uiState.update { it.copy(isCreating = false, showCreateDialog = false, message = "Terra creada correctament") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Editar ──
    fun startEdit(terra: TerraFullDto) {
        _uiState.update {
            it.copy(
                editingTerra = terra,
                editTitularId = terra.titular_id ?: "",
                editMunicipiLiteral = terra.municipi_literal ?: "",
                editUsSigpac = terra.us_sigpac ?: "",
                editCultiu = terra.cultiu ?: "",
                editSuperficie = (terra.superficie ?: 0.0).toString(),
                editZona = terra.zona
            )
        }
    }

    fun cancelEdit() { _uiState.update { it.copy(editingTerra = null) } }

    fun onEditTitularId(v: String) { _uiState.update { it.copy(editTitularId = v) } }
    fun onEditMunicipiLiteral(v: String) { _uiState.update { it.copy(editMunicipiLiteral = v) } }
    fun onEditUsSigpac(v: String) { _uiState.update { it.copy(editUsSigpac = v) } }
    fun onEditCultiu(v: String) { _uiState.update { it.copy(editCultiu = v) } }
    fun onEditSuperficie(v: String) { _uiState.update { it.copy(editSuperficie = v) } }
    fun onEditZona(v: String) { _uiState.update { it.copy(editZona = v) } }

    fun saveEdit() {
        val s = _uiState.value
        val terra = s.editingTerra ?: return
        val superficie = s.editSuperficie.toDoubleOrNull()
        if (superficie == null) {
            _uiState.update { it.copy(message = "Superficie ha de ser un nombre") }
            return
        }
        if (s.editZona != "ZV" && s.editZona != "ZNV") {
            _uiState.update { it.copy(message = "La zona ha de ser ZV o ZNV") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isEditing = true) }
            try {
                val body = TerraUpdateFullRequest(
                    titular_id = s.editTitularId.ifBlank { null },
                    municipi_literal = s.editMunicipiLiteral.ifBlank { null },
                    us_sigpac = s.editUsSigpac.ifBlank { null },
                    cultiu = s.editCultiu.ifBlank { null },
                    superficie = superficie,
                    zona = s.editZona
                )
                repository.updateTerra(terra.id, body)
                _uiState.update { it.copy(isEditing = false, editingTerra = null, message = "Terra actualitzada") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isEditing = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Eliminar ──
    fun deleteTerra(terraId: String) {
        scope.launch {
            try {
                repository.deleteTerra(terraId)
                _uiState.update { it.copy(message = "Terra eliminada") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error eliminant: ${e.message}") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    fun clear() { scope.cancel() }
}

