package cat.agrisync.viewmodel

import cat.agrisync.data.OficinaCreateRequest
import cat.agrisync.data.OficinaDto
import cat.agrisync.data.OficinaRepository
import cat.agrisync.data.OficinaUpdateRequest
import cat.agrisync.data.TecnicDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OficinaManagementUiState(
    val oficines: List<OficinaDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    // Crear
    val showCreateDialog: Boolean = false,
    val newNom: String = "",
    val isCreating: Boolean = false,
    // Editar
    val editingOficina: OficinaDto? = null,
    val editNom: String = "",
    val isEditing: Boolean = false
)

internal class OficinaManagementViewModel(
    private val repository: OficinaRepository,
    private val currentTecnic: TecnicDto
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(OficinaManagementUiState())
    val uiState: StateFlow<OficinaManagementUiState> = _uiState.asStateFlow()
    private fun isAdmin(): Boolean = currentTecnic.rol == "admin"
    private fun canEditOficina(oficina: OficinaDto): Boolean {
        return isAdmin() || (currentTecnic.rol == "oficina_manager" && oficina.id == currentTecnic.oficina_id)
    }

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val list = repository.listAll().let { oficines ->
                    if (isAdmin()) oficines else oficines.filter { it.id == currentTecnic.oficina_id }
                }
                _uiState.update { it.copy(isLoading = false, oficines = list) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error carregant oficines") }
            }
        }
    }

    // ── Crear ──
    fun showCreateDialog() { _uiState.update { it.copy(showCreateDialog = true, newNom = "") } }
    fun hideCreateDialog() { _uiState.update { it.copy(showCreateDialog = false) } }
    fun onNewNom(v: String) { _uiState.update { it.copy(newNom = v) } }

    fun createOficina() {
        if (!isAdmin()) {
            _uiState.update { it.copy(message = "Nomes l'administrador pot crear oficines") }
            return
        }
        val nom = _uiState.value.newNom.trim()
        if (nom.isBlank()) {
            _uiState.update { it.copy(message = "El nom es obligatori") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                repository.create(OficinaCreateRequest(nom = nom))
                _uiState.update { it.copy(isCreating = false, showCreateDialog = false, message = "Oficina creada") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreating = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Editar ──
    fun startEdit(oficina: OficinaDto) {
        if (!canEditOficina(oficina)) {
            _uiState.update { it.copy(message = "Nomes pots editar la teva oficina") }
            return
        }
        _uiState.update { it.copy(editingOficina = oficina, editNom = oficina.nom) }
    }
    fun cancelEdit() { _uiState.update { it.copy(editingOficina = null) } }
    fun onEditNom(v: String) { _uiState.update { it.copy(editNom = v) } }

    fun saveEdit() {
        val oficina = _uiState.value.editingOficina ?: return
        if (!canEditOficina(oficina)) {
            _uiState.update { it.copy(isEditing = false, editingOficina = null, message = "Nomes pots editar la teva oficina") }
            return
        }
        val nom = _uiState.value.editNom.trim()
        if (nom.isBlank()) {
            _uiState.update { it.copy(message = "El nom es obligatori") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isEditing = true) }
            try {
                repository.update(oficina.id, OficinaUpdateRequest(nom = nom))
                _uiState.update { it.copy(isEditing = false, editingOficina = null, message = "Oficina actualitzada") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isEditing = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Eliminar ──
    fun deleteOficina(id: String) {
        if (!isAdmin()) {
            _uiState.update { it.copy(message = "Nomes l'administrador pot eliminar oficines") }
            return
        }
        scope.launch {
            try {
                repository.delete(id)
                _uiState.update { it.copy(message = "Oficina eliminada") }
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error: ${e.message}. Pot ser que tingui tecnics assignats.") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }
    fun clear() { scope.cancel() }
}

