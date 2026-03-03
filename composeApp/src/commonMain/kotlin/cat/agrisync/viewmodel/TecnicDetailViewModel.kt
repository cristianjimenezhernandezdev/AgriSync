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
    val oficines: List<OficinaDto> = emptyList(),
    val assignacions: List<TecnicTitularWithTitular> = emptyList(),
    val allTitulars: List<TitularDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    // Edit fields
    val editNom: String = "",
    val editEmail: String = "",
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
    private val repository: TecnicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TecnicDetailUiState())
    val uiState: StateFlow<TecnicDetailUiState> = _uiState.asStateFlow()
    private var tecnicId: String = ""

    fun load(id: String) {
        tecnicId = id
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tecnics: List<TecnicDto> = repository.listAll()
                val tecnic = tecnics.find { it.id == id }
                    ?: throw IllegalStateException("Tecnic no trobat")
                val oficines = repository.listOficines()
                val assignacions = repository.listAssignacions(id)
                val titulars = repository.listAllTitulars()

                _uiState.update {
                    it.copy(
                        isLoading = false, tecnic = tecnic, oficines = oficines,
                        assignacions = assignacions, allTitulars = titulars,
                        editNom = tecnic.nom, editEmail = tecnic.email ?: "",
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
    fun onEditRol(v: String) { _uiState.update { it.copy(editRol = v) } }
    fun onEditOficina(v: String) { _uiState.update { it.copy(editOficinaId = v) } }
    fun onNewTitular(v: String) { _uiState.update { it.copy(newTitularId = v) } }
    fun onNewScope(v: String) { _uiState.update { it.copy(newScope = v) } }
    fun onNewPassword(v: String) { _uiState.update { it.copy(newPassword = v) } }
    fun togglePasswordField() { _uiState.update { it.copy(showPasswordField = !it.showPasswordField, newPassword = "") } }

    fun changePassword() {
        val st = _uiState.value
        val userId = st.tecnic?.user_id
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
        scope.launch {
            try {
                val updated = repository.updateTecnic(tecnicId, TecnicUpdateRequest(
                    nom = st.editNom.trim(), email = st.editEmail.trim(),
                    rol = st.editRol, oficina_id = st.editOficinaId
                ))
                _uiState.update { it.copy(tecnic = updated, message = "Tecnic guardat") }
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun addAssignacio() {
        val st = _uiState.value
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

