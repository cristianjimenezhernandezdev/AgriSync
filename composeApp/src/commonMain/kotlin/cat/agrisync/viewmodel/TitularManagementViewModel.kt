package cat.agrisync.viewmodel

import cat.agrisync.data.TitularCreateRequest
import cat.agrisync.data.TitularDto
import cat.agrisync.data.OficinaDto
import cat.agrisync.data.OficinaTitularCompartitCreateRequest
import cat.agrisync.data.OficinaTitularCompartitDto
import cat.agrisync.data.TitularManagementRepository
import cat.agrisync.data.TitularUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TitularManagementUiState(
    val titulars: List<TitularDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    // Crear
    val showCreateDialog: Boolean = false,
    val newNom: String = "",
    val newNif: String = "",
    val isCreating: Boolean = false,
    // Editar
    val editingTitular: TitularDto? = null,
    val editNom: String = "",
    val editNif: String = "",
    val isEditing: Boolean = false,
    // Compartir
    val oficines: List<OficinaDto> = emptyList(),
    val shareTargetTitular: TitularDto? = null,
    val officeShares: List<OficinaTitularCompartitDto> = emptyList(),
    val showShareDialog: Boolean = false,
    val newShareOficinaId: String = "",
    val newShareScope: String = "lectura",
    val isSharing: Boolean = false,
    // Paginació
    val currentPage: Int = 0,
    val pageSize: Int = 15
) {
    val filtered: List<TitularDto>
        get() {
            if (searchQuery.isBlank()) return titulars
            val q = searchQuery.uppercase().replace(" ", "").replace(".", "").replace("-", "")
            return titulars.filter {
                val nif = (it.nif ?: "").uppercase().replace(" ", "").replace(".", "").replace("-", "")
                nif.contains(q) || it.nom_rao.contains(searchQuery, ignoreCase = true)
            }
        }

    val totalPages: Int
        get() = if (filtered.isEmpty()) 1 else ((filtered.size - 1) / pageSize) + 1

    val pageItems: List<TitularDto>
        get() {
            val start = currentPage * pageSize
            if (start >= filtered.size) return emptyList()
            return filtered.subList(start, minOf(filtered.size, start + pageSize))
        }
}

internal class TitularManagementViewModel(
    private val repository: TitularManagementRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TitularManagementUiState())
    val uiState: StateFlow<TitularManagementUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val list = repository.listAll()
                val oficines = repository.listOficines()
                _uiState.update { it.copy(isLoading = false, titulars = list, oficines = oficines, currentPage = 0) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error carregant titulars") }
            }
        }
    }

    fun onSearchChange(value: String) {
        _uiState.update { it.copy(searchQuery = value, currentPage = 0) }
    }

    fun nextPage() {
        _uiState.update { s -> if (s.currentPage + 1 < s.totalPages) s.copy(currentPage = s.currentPage + 1) else s }
    }

    fun prevPage() {
        _uiState.update { s -> if (s.currentPage > 0) s.copy(currentPage = s.currentPage - 1) else s }
    }

    // ── Crear ──
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, newNom = "", newNif = "") }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun onNewNom(value: String) { _uiState.update { it.copy(newNom = value) } }
    fun onNewNif(value: String) { _uiState.update { it.copy(newNif = value) } }

    fun createTitular() {
        val state = _uiState.value
        if (state.newNom.isBlank()) {
            _uiState.update { it.copy(message = "El nom es obligatori") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                val body = TitularCreateRequest(
                    nif = state.newNif.ifBlank { null },
                    nom_rao = state.newNom.trim()
                )
                repository.create(body)
                _uiState.update { it.copy(isCreating = false, showCreateDialog = false, message = "Titular creat correctament") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Editar ──
    fun startEdit(titular: TitularDto) {
        _uiState.update { it.copy(editingTitular = titular, editNom = titular.nom_rao, editNif = titular.nif ?: "") }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editingTitular = null) }
    }

    fun onEditNom(value: String) { _uiState.update { it.copy(editNom = value) } }
    fun onEditNif(value: String) { _uiState.update { it.copy(editNif = value) } }

    fun saveEdit() {
        val state = _uiState.value
        val titular = state.editingTitular ?: return
        if (state.editNom.isBlank()) {
            _uiState.update { it.copy(message = "El nom es obligatori") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isEditing = true) }
            try {
                val body = TitularUpdateRequest(
                    nif = state.editNif.ifBlank { null },
                    nom_rao = state.editNom.trim()
                )
                repository.update(titular.id, body)
                _uiState.update { it.copy(isEditing = false, editingTitular = null, message = "Titular actualitzat") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isEditing = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Eliminar ──
    fun deleteTitular(titularId: String) {
        scope.launch {
            try {
                repository.delete(titularId)
                _uiState.update { it.copy(message = "Titular eliminat") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error eliminant: ${e.message}") }
            }
        }
    }

    fun openShareDialog(titular: TitularDto) {
        scope.launch {
            try {
                val shares = repository.listOfficeShares(titular.id)
                val availableOfficeId = _uiState.value.oficines.firstOrNull()?.id ?: ""
                _uiState.update {
                    it.copy(
                        shareTargetTitular = titular,
                        officeShares = shares,
                        newShareOficinaId = availableOfficeId,
                        newShareScope = "lectura",
                        showShareDialog = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error carregant comparticions: ${e.message}") }
            }
        }
    }

    fun closeShareDialog() {
        _uiState.update {
            it.copy(
                shareTargetTitular = null,
                officeShares = emptyList(),
                showShareDialog = false,
                isSharing = false
            )
        }
    }

    fun onNewShareOficina(value: String) {
        _uiState.update { it.copy(newShareOficinaId = value) }
    }

    fun onNewShareScope(value: String) {
        _uiState.update { it.copy(newShareScope = value) }
    }

    fun createOfficeShare() {
        val state = _uiState.value
        val titular = state.shareTargetTitular ?: return
        if (state.newShareOficinaId.isBlank()) {
            _uiState.update { it.copy(message = "Has de seleccionar una oficina") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isSharing = true) }
            try {
                repository.createOfficeShare(
                    OficinaTitularCompartitCreateRequest(
                        oficina_id = state.newShareOficinaId,
                        titular_id = titular.id,
                        scope = state.newShareScope
                    )
                )
                val shares = repository.listOfficeShares(titular.id)
                val availableOfficeId = _uiState.value.oficines.firstOrNull()?.id ?: ""
                _uiState.update {
                    it.copy(
                        officeShares = shares,
                        newShareOficinaId = availableOfficeId,
                        isSharing = false,
                        message = "Comparticio creada"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSharing = false, message = "Error: ${e.message}") }
            }
        }
    }

    fun deleteOfficeShare(shareId: String) {
        val titular = _uiState.value.shareTargetTitular ?: return
        scope.launch {
            try {
                repository.deleteOfficeShare(shareId)
                val shares = repository.listOfficeShares(titular.id)
                _uiState.update { it.copy(officeShares = shares, message = "Comparticio eliminada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error eliminant comparticio: ${e.message}") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    fun clear() { scope.cancel() }
}

