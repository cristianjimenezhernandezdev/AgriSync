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

data class TecnicManagementUiState(
    val tecnics: List<TecnicDto> = emptyList(),
    val oficines: List<OficinaDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    // Formulari nou tècnic
    val showCreateDialog: Boolean = false,
    val newNom: String = "",
    val newEmail: String = "",
    val newTelefon: String = "",
    val newPassword: String = "",
    val newOficinaId: String = "",
    val newRol: String = "tecnic",
    val isCreating: Boolean = false,
    // Reset password
    val showPasswordDialog: Boolean = false,
    val passwordTecnic: TecnicDto? = null,
    val resetPassword: String = "",
    val resetPasswordConfirm: String = "",
    val isResettingPassword: Boolean = false,
    // Delete tecnic
    val showDeleteDialog: Boolean = false,
    val deleteTecnic: TecnicDto? = null,
    val isDeleting: Boolean = false
)

internal class TecnicManagementViewModel(
    private val repository: TecnicRepository,
    private val currentTecnic: TecnicDto
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TecnicManagementUiState())
    val uiState: StateFlow<TecnicManagementUiState> = _uiState.asStateFlow()
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

    private fun filterManagedTecnics(tecnics: List<TecnicDto>): List<TecnicDto> {
        return if (isAdmin()) tecnics else tecnics.filter(::canManageTecnic)
    }

    private fun filterManagedOficines(oficines: List<OficinaDto>): List<OficinaDto> {
        return if (isAdmin()) oficines else oficines.filter { it.id == currentTecnic.oficina_id }
    }

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val tecnics = filterManagedTecnics(repository.listAll())
                val oficines = filterManagedOficines(repository.listOficines())
                val defaultOficinaId = if (currentTecnic.rol == "oficina_manager") {
                    currentTecnic.oficina_id
                } else {
                    oficines.firstOrNull()?.id ?: ""
                }
                _uiState.update {
                    it.copy(isLoading = false, tecnics = tecnics, oficines = oficines,
                        newOficinaId = defaultOficinaId)
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isLoading = false, error = ex.message ?: "Error") }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update {
            it.copy(
                showCreateDialog = true,
                newNom = "",
                newEmail = "",
                newTelefon = "",
                newPassword = "",
                newRol = "tecnic",
                newOficinaId = if (currentTecnic.rol == "oficina_manager") currentTecnic.oficina_id else it.newOficinaId
            )
        }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false) }
    }

    fun onNewNom(v: String) { _uiState.update { it.copy(newNom = v) } }
    fun onNewEmail(v: String) { _uiState.update { it.copy(newEmail = v) } }
    fun onNewTelefon(v: String) { _uiState.update { it.copy(newTelefon = v) } }
    fun onNewPassword(v: String) { _uiState.update { it.copy(newPassword = v) } }
    fun onNewOficina(v: String) { _uiState.update { it.copy(newOficinaId = v) } }
    fun onNewRol(v: String) { _uiState.update { it.copy(newRol = v) } }

    fun createTecnic() {
        val st = _uiState.value
        if (st.newNom.isBlank() || st.newEmail.isBlank() || st.newPassword.isBlank() || st.newOficinaId.isBlank()) {
            _uiState.update { it.copy(message = "Tots els camps son obligatoris") }
            return
        }
        if (st.newPassword.length < 6) {
            _uiState.update { it.copy(message = "El password ha de tenir minim 6 caracters") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                val cleanEmail = st.newEmail.trim().lowercase()
                val targetOficinaId = if (currentTecnic.rol == "oficina_manager") {
                    currentTecnic.oficina_id
                } else {
                    st.newOficinaId
                }
                val targetRol = when {
                    currentTecnic.rol == "oficina_manager" && st.newRol !in managerManagedRoles -> "tecnic"
                    else -> st.newRol
                }

                val existingTecnic = repository.findVisibleTecnicByEmail(cleanEmail)
                if (existingTecnic != null) {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            message = "Ja existeix un tecnic visible amb aquest email: ${existingTecnic.nom}"
                        )
                    }
                    return@launch
                }

                // 1) Crear usuari Auth
                val authLink = repository.createAuthUser(cleanEmail, st.newPassword)
                val userId = authLink.userId
                println("[TECNIC] Auth user ${if (authLink.reusedExisting) "recuperat" else "creat"}: $userId")

                // 2) Crear tècnic a public.tecnic vinculat a l'usuari Auth
                try {
                    repository.createTecnicWithServiceRole(TecnicCreateRequest(
                        oficina_id = targetOficinaId,
                        user_id = userId,
                        nom = st.newNom.trim(),
                        email = cleanEmail,
                        telefon = st.newTelefon.trim().ifBlank { null },
                        rol = targetRol,
                        actiu = true
                    ))
                } catch (ex: Exception) {
                    if (!authLink.reusedExisting) {
                        runCatching { repository.deleteAuthUser(userId) }
                        println("[TECNIC] Rollback Auth user: $userId")
                    }
                    throw ex
                }
                println("[TECNIC] Tecnic creat: ${st.newNom}")

                // 3) Recarregar llista
                val tecnics = filterManagedTecnics(repository.listAll())
                _uiState.update {
                    it.copy(
                        isCreating = false, showCreateDialog = false,
                        tecnics = tecnics,
                        message = "Tecnic '${st.newNom}' creat correctament" +
                            if (authLink.reusedExisting) " reutilitzant un login Auth ja existent" else ""
                    )
                }
            } catch (ex: Exception) {
                println("[TECNIC] Error: ${ex.message}")
                _uiState.update { it.copy(isCreating = false, message = "Error: ${ex.message}") }
            }
        }
    }

    fun toggleActiu(tecnic: TecnicDto) {
        scope.launch {
            try {
                if (!canManageTecnic(tecnic)) {
                    _uiState.update { it.copy(message = "Nomes pots gestionar tecnics de la teva oficina") }
                    return@launch
                }
                if (tecnic.rol.equals("admin", ignoreCase = true)) {
                    _uiState.update { it.copy(message = "L'administrador no es pot desactivar") }
                    return@launch
                }
                println("[TECNIC] toggleActiu: ${tecnic.id} actiu=${tecnic.actiu} -> ${!tecnic.actiu}")
                val updated = repository.updateTecnic(tecnic.id, TecnicUpdateRequest(actiu = !tecnic.actiu))
                println("[TECNIC] toggleActiu OK: ${updated.nom} actiu=${updated.actiu}")
                _uiState.update { st ->
                    st.copy(
                        tecnics = st.tecnics.map { if (it.id == tecnic.id) updated else it },
                        message = "${updated.nom} ${if (updated.actiu) "activat" else "desactivat"}"
                    )
                }
            } catch (ex: Exception) {
                println("[TECNIC] toggleActiu ERROR: ${ex.message}")
                ex.printStackTrace()
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun updateTecnic(tecnicId: String, nom: String, email: String, telefon: String, rol: String, oficinaId: String) {
        scope.launch {
            try {
                val current = _uiState.value.tecnics.find { it.id == tecnicId }
                if (current != null && !canManageTecnic(current)) {
                    _uiState.update { it.copy(message = "Nomes pots gestionar tecnics de la teva oficina") }
                    return@launch
                }
                val targetOficinaId = if (isOfficeManager()) currentTecnic.oficina_id else oficinaId
                val targetRol = if (isOfficeManager() && rol !in managerManagedRoles) "tecnic" else rol
                val updated = repository.updateTecnic(tecnicId, TecnicUpdateRequest(
                    nom = nom,
                    email = email,
                    telefon = telefon.ifBlank { null },
                    rol = targetRol,
                    oficina_id = targetOficinaId
                ))
                _uiState.update { st ->
                    st.copy(
                        tecnics = filterManagedTecnics(st.tecnics.map { if (it.id == tecnicId) updated else it }),
                        message = "Tecnic '${updated.nom}' actualitzat"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(message = "Error: ${ex.message}") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun showDeleteDialog(tecnic: TecnicDto) {
        if (!canManageTecnic(tecnic)) {
            _uiState.update { it.copy(message = "Nomes pots eliminar tecnics de la teva oficina") }
            return
        }
        _uiState.update { it.copy(showDeleteDialog = true, deleteTecnic = tecnic) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, deleteTecnic = null) }
    }

    fun confirmDeleteTecnic() {
        val st = _uiState.value
        val tecnic = st.deleteTecnic ?: return
        if (!canManageTecnic(tecnic)) {
            _uiState.update {
                it.copy(
                    showDeleteDialog = false,
                    deleteTecnic = null,
                    isDeleting = false,
                    message = "Nomes pots eliminar tecnics de la teva oficina"
                )
            }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                repository.deleteTecnic(tecnic.id)
                var message = "Tecnic '${tecnic.nom}' eliminat correctament"

                if (tecnic.user_id != null) {
                    try {
                        repository.deleteAuthUser(tecnic.user_id)
                        message += " i usuari Auth eliminat"
                    } catch (authEx: Exception) {
                        message += ". El registre funcional s'ha eliminat, pero l'usuari Auth no s'ha pogut esborrar: ${authEx.message}"
                    }
                }

                val tecnics = filterManagedTecnics(repository.listAll())
                _uiState.update {
                    it.copy(
                        tecnics = tecnics,
                        showDeleteDialog = false,
                        deleteTecnic = null,
                        isDeleting = false,
                        message = message
                    )
                }
            } catch (ex: Exception) {
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        message = "Error eliminant tecnic: ${ex.message}"
                    )
                }
            }
        }
    }

    // ── Reset password ──
    fun showPasswordDialog(tecnic: TecnicDto) {
        if (!canManageTecnic(tecnic)) {
            _uiState.update { it.copy(message = "Nomes pots canviar el password de tecnics de la teva oficina") }
            return
        }
        _uiState.update { it.copy(showPasswordDialog = true, passwordTecnic = tecnic, resetPassword = "", resetPasswordConfirm = "") }
    }

    fun hidePasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false, passwordTecnic = null) }
    }

    fun onResetPassword(v: String) { _uiState.update { it.copy(resetPassword = v) } }
    fun onResetPasswordConfirm(v: String) { _uiState.update { it.copy(resetPasswordConfirm = v) } }

    fun confirmResetPassword() {
        val st = _uiState.value
        val tecnic = st.passwordTecnic ?: return
        if (!canManageTecnic(tecnic)) {
            _uiState.update {
                it.copy(
                    isResettingPassword = false,
                    showPasswordDialog = false,
                    passwordTecnic = null,
                    message = "Nomes pots canviar el password de tecnics de la teva oficina"
                )
            }
            return
        }
        val userId = tecnic.user_id
        if (userId == null) {
            _uiState.update { it.copy(message = "'${tecnic.nom}' no te compte Auth (sense user_id)") }
            return
        }
        if (st.resetPassword.length < 6) {
            _uiState.update { it.copy(message = "El password ha de tenir minim 6 caracters") }
            return
        }
        if (st.resetPassword != st.resetPasswordConfirm) {
            _uiState.update { it.copy(message = "Els passwords no coincideixen") }
            return
        }
        scope.launch {
            _uiState.update { it.copy(isResettingPassword = true) }
            try {
                repository.updateAuthUserPassword(userId, st.resetPassword)
                _uiState.update {
                    it.copy(
                        isResettingPassword = false,
                        showPasswordDialog = false,
                        passwordTecnic = null,
                        message = "Password de '${tecnic.nom}' canviat correctament. Ara pot entrar amb: ${tecnic.email} / (nou password)"
                    )
                }
            } catch (ex: Exception) {
                _uiState.update { it.copy(isResettingPassword = false, message = "Error: ${ex.message}") }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}

