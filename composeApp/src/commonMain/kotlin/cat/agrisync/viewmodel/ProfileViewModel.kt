package cat.agrisync.viewmodel

import cat.agrisync.data.AuthService
import cat.agrisync.data.TecnicDto
import cat.agrisync.data.TecnicRepository
import cat.agrisync.data.TecnicUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val nom: String = "",
    val email: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    // Password
    val showPasswordDialog: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isChangingPassword: Boolean = false,
    // Feedback
    val message: String? = null
)

internal class ProfileViewModel(
    private val tecnic: TecnicDto,
    private val tecnicRepository: TecnicRepository,
    private val authService: AuthService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(
        ProfileUiState(
            nom = tecnic.nom,
            email = tecnic.email ?: ""
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // ── Edició de dades ──

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true, nom = tecnic.nom, email = tecnic.email ?: "") }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(isEditing = false, nom = tecnic.nom, email = tecnic.email ?: "") }
    }

    fun onNomChange(value: String) { _uiState.update { it.copy(nom = value) } }
    fun onEmailChange(value: String) { _uiState.update { it.copy(email = value) } }

    fun saveProfile() {
        val state = _uiState.value
        if (state.nom.isBlank()) {
            _uiState.update { it.copy(message = "El nom es obligatori") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val body = TecnicUpdateRequest(
                    nom = state.nom.trim(),
                    email = state.email.trim().ifBlank { null }
                )
                tecnicRepository.updateTecnic(tecnic.id, body)
                // Recarregar les dades al AuthService perquè es reflecteixin a tota l'app
                authService.reloadTecnic()
                _uiState.update { it.copy(isSaving = false, isEditing = false, message = "Perfil actualitzat correctament") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, message = "Error: ${e.message}") }
            }
        }
    }

    // ── Canvi de password ──

    fun showPasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = true, currentPassword = "", newPassword = "", confirmPassword = "") }
    }

    fun hidePasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false) }
    }

    fun onCurrentPasswordChange(value: String) { _uiState.update { it.copy(currentPassword = value) } }
    fun onNewPasswordChange(value: String) { _uiState.update { it.copy(newPassword = value) } }
    fun onConfirmPasswordChange(value: String) { _uiState.update { it.copy(confirmPassword = value) } }

    fun changePassword() {
        val state = _uiState.value
        if (state.newPassword.length < 6) {
            _uiState.update { it.copy(message = "El password ha de tenir minim 6 caracters") }
            return
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(message = "Els passwords no coincideixen") }
            return
        }
        val userId = tecnic.user_id
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(message = "Aquest tecnic no te compte Auth associat") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isChangingPassword = true) }
            try {
                tecnicRepository.updateAuthUserPassword(userId, state.newPassword)
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        showPasswordDialog = false,
                        message = "Password canviat correctament"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isChangingPassword = false, message = "Error: ${e.message}") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    fun clear() { scope.cancel() }
}

