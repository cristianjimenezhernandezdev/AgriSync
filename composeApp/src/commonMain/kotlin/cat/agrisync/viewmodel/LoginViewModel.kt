package cat.agrisync.viewmodel

import cat.agrisync.data.ApiException
import cat.agrisync.data.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

internal class LoginViewModel(
    private val authService: AuthService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Email i password obligatoris") }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                println("[LOGIN] Intentant login amb email=$email")
                authService.login(email, password)
                println("[LOGIN] Login OK — authState hauria de ser Authenticated")
                _uiState.update { it.copy(isLoading = false, password = "") }
            } catch (ex: ApiException) {
                println("[LOGIN] ApiException: ${ex.statusCode} — ${ex.message}")
                authService.signOut()
                val msg = when (ex.statusCode) {
                    400 -> "Credencials incorrectes"
                    401, 403 -> "Sense permis"
                    else -> ex.message?.ifBlank { null } ?: "Error (HTTP ${ex.statusCode})"
                }
                _uiState.update { it.copy(isLoading = false, error = msg) }
            } catch (ex: IllegalArgumentException) {
                println("[LOGIN] IllegalArgumentException: ${ex.message}")
                authService.signOut()
                _uiState.update { it.copy(isLoading = false, error = "Email i password obligatoris") }
            } catch (ex: Exception) {
                println("[LOGIN] Exception: ${ex::class.simpleName} — ${ex.message}")
                ex.printStackTrace()
                authService.signOut()
                _uiState.update { it.copy(isLoading = false, error = ex.message ?: "Error de xarxa") }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
