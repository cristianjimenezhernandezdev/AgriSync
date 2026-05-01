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

data class TecnicDetailUiState(
    val tecnic: TecnicDto? = null,
    val updatedByLabel: String? = null,
    val oficines: List<OficinaDto> = emptyList(),
    val assignacions: List<TecnicTitularWithTitular> = emptyList(),
    val allTitulars: List<TitularDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    // Edit fields
    val editNom: String = "",
    val editEmail: String = "",
    val editTelefon: String = "",
    val editRol: String = "tecnic",
    val editOficinaId: String = "",
    // Add assignacio
    val newTitularId: String = "",
    val newScope: String = "comu",
    // Change password
    val newPassword: String = "",
    val showPasswordField: Boolean = false
)

internal class TecnicDetailViewModel(
    private val repository: TecnicRepository,
    private val currentTecnic: TecnicDto
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TecnicDetailUiState())
    val uiState: StateFlow<TecnicDetailUiState> = _uiState.asStateFlow()
    private var tecnicId: String = ""
    private val managerManagedRoles = setOf("tecnic", "lectura")

    private fun isAdmin(): Boolean = currentTecnic.rol == "admin"

    private fun isOfficeManager(): Boolean = currentTecnic.rol == "oficina_manager"

    private fun canManageTecnic(tecnic: TecnicDto): Boolean {
        return isAdmin() || (
            isOfficeManager() &&
                tecnic.oficina_id == currentTecnic.oficina_id &&
                (tecnic.rol ?: "tecnic") in managerManagedRoles
        )
    }

    private fun filterManagedOficines(oficines: List<OficinaDto>): List<OficinaDto> {
        return if (isAdmin()) oficines else oficines.filter { it.id == currentTecnic.oficina_id }
    }

    fun load(id: String) {
        tecnicId = id
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tecnics: List<TecnicDto> = repository.listAll()
                val tecnic = tecnics.find { it.id == id }
                    ?: throw IllegalStateException("Tecnic no trobat")
                if (!canManageTecnic(tecnic)) {
                    throw IllegalStateException("No pots gestionar tecnics d'altres oficines")
                }
                val oficines = filterManagedOficines(repository.listOficines())
                val assignacions = repository.listAssignacions(id)
                val titulars = repository.listAllTitulars()
                val updatedByLabel = repository.resolveActorLabel(tecnic.updated_by)

                _uiState.update {
                    it.copy(
                        isLoading = false, tecnic = tecnic, oficines = oficines,
                        updatedByLabel = updatedByLabel,
                        assignacions = assignacions, allTitulars = titulars,
                        editNom = tecnic.nom, editEmail = tecnic.email ?: "",
                        editTelefon = tecnic.telefon ?: "",
                        editRol = tecnic.rol ?: "tecnic", editOficinaId = tecnic.oficina_id,
                        newTitularId = titulars.firstOrNull()?.id ?: ""
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ex.message ?: "Error") }
            }
        }
    }

    fun onEditNom(v: String) { _uiState.update { it.copy(editNom = v) } }
    fun onEditEmail(v: String) { _uiState.update { it.copy(editEmail = v) } }
    fun onEditTelefon(v: String) { _uiState.update { it.copy(editTelefon = v) } }
    fun onEditRol(v: String) {
        if (isOfficeManager() && v !in managerManagedRoles) return
        _uiState.update { it.copy(editRol = v) }
    }
    fun onEditOficina(v: String) {
        if (isOfficeManager() && v != currentTecnic.oficina_id) return
        _uiState.update { it.copy(editOficinaId = v) }
    }
    fun onNewTitular(v: String) { _uiState.update { it.copy(newTitularId = v) } }
    fun onNewScope(v: String) { _uiState.update { it.copy(newScope = v) } }
    fun onNewPassword(v: String) { _uiState.update { it.copy(newPassword = v) } }
    fun togglePasswordField() { _uiState.update { it.copy(showPasswordField = !it.showPasswordField, newPassword = "") } }

    fun changePassword() {
        val st = _uiState.value
        val tecnic = st.tecnic ?: return
        if (!canManageTecnic(tecnic)) {
            _uiState.update { it.copy(message = "Nomes pots canviar el password de tecnics de la teva oficina") }
            return
        }
        val userId = tecnic.user_id
        if (userId == null) {
            _uiState.update { it.copy(message = "Aquest tecnic no te login (sense user_id)") }
            return
        }
        if (st.newPassword.length < 6) {
            _uiState.update { it.copy(message = "El password ha de tenir minim 6 caracters") }
            return
        }
        scope.launch {
            try {
                repository.updateAuthUserPassword(userId, st.newPassword)
                _uiState.update { it.copy(newPassword = "", showPasswordField = false, message = "Password canviat correctament") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun saveTecnic() {
        val st = _uiState.value
        val tecnic = st.tecnic ?: return
        if (!canManageTecnic(tecnic)) {
            _uiState.update { it.copy(message = "Nomes pots gestionar tecnics de la teva oficina") }
            return
        }
        val targetRol = if (isOfficeManager() && st.editRol !in managerManagedRoles) "tecnic" else st.editRol
        val targetOficinaId = if (isOfficeManager()) currentTecnic.oficina_id else st.editOficinaId
        scope.launch {
            try {
                val updated = repository.updateTecnic(tecnicId, TecnicUpdateRequest(
                    nom = st.editNom.trim(), email = st.editEmail.trim(),
                    telefon = st.editTelefon.trim().ifBlank { null },
                    rol = targetRol, oficina_id = targetOficinaId
                ))
                val updatedByLabel = repository.resolveActorLabel(updated.updated_by)
                _uiState.update {
                    it.copy(
                        tecnic = updated,
                        updatedByLabel = updatedByLabel,
                        message = "Tecnic guardat"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun addAssignacio() {
        val st = _uiState.value
        val tecnic = st.tecnic ?: return
        if (!canManageTecnic(tecnic)) {
            _uiState.update { it.copy(message = "Nomes pots gestionar assignacions dels teus tecnics") }
            return
        }
        if (st.newTitularId.isBlank()) return
        scope.launch {
            try {
                repository.createAssignacio(TecnicTitularAssignRequest(
                    tecnic_id = tecnicId,
                    titular_id = st.newTitularId,
                    scope = st.newScope,
                    actiu = true
                ))
                val assignacions = repository.listAssignacions(tecnicId)
                _uiState.update { it.copy(assignacions = assignacions, message = "Assignacio creada") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun deleteAssignacio(assignacioId: String) {
        val tecnic = _uiState.value.tecnic ?: return
        if (!canManageTecnic(tecnic)) {
            _uiState.update { it.copy(message = "Nomes pots eliminar assignacions dels teus tecnics") }
            return
        }
        scope.launch {
            try {
                repository.deleteAssignacio(assignacioId)
                val assignacions = repository.listAssignacions(tecnicId)
                _uiState.update { it.copy(assignacions = assignacions, message = "Assignacio eliminada") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clear() {
        scope.cancel()
    }
}

