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
import kotlin.coroutines.cancellation.CancellationException

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
                val msg = ex.toUserLoginMessage()
                _uiState.update { it.copy(isLoading = false, error = msg) }
            } catch (ex: IllegalArgumentException) {
                println("[LOGIN] IllegalArgumentException: ${ex.message}")
                authService.signOut()
                _uiState.update { it.copy(isLoading = false, error = "Email i password obligatoris") }
            } catch (ex: Exception) {
                println("[LOGIN] Exception: ${ex::class.simpleName} — ${ex.message}")
                ex.printStackTrace()
                authService.signOut()
                _uiState.update { it.copy(isLoading = false, error = ex.toUserLoginMessage()) }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }

    private fun ApiException.toUserLoginMessage(): String {
        val rawMessage = message.orEmpty()
        return when {
            statusCode == 400 -> "Credencials incorrectes"
            statusCode == 401 || statusCode == 403 -> "Sense permis"
            rawMessage.contains("Database error querying schema", ignoreCase = true) ||
                rawMessage.contains("Database error finding users", ignoreCase = true) ->
                "Error intern de Supabase Auth. Recrea els usuaris a Authentication i torna a executar el seed."
            else -> message?.ifBlank { null } ?: "Error (HTTP $statusCode)"
        }
    }

    private fun Throwable.toUserLoginMessage(): String {
        if (this is CancellationException) throw this

        val rawMessage = debugMessage()
        return when {
            rawMessage.contains("getsockopt", ignoreCase = true) ||
                rawMessage.contains("connection refused", ignoreCase = true) ->
                "No s'ha pogut connectar amb Supabase. Revisa connexio, firewall o proxy, i confirma que SUPABASE_URL sigui accessible."
            rawMessage.contains("timed out", ignoreCase = true) ->
                "Temps d'espera esgotat connectant amb Supabase. Revisa la connexio o torna-ho a provar."
            rawMessage.contains("unknown host", ignoreCase = true) ||
                rawMessage.contains("unresolved", ignoreCase = true) ->
                "No s'ha pogut resoldre l'adreca de Supabase. Revisa internet i SUPABASE_URL."
            rawMessage.contains("ssl", ignoreCase = true) ||
                rawMessage.contains("handshake", ignoreCase = true) ->
                "Error SSL connectant amb Supabase. Revisa certificats, antivirus o inspeccio HTTPS."
            else -> "Error de xarxa: ${rawMessage.ifBlank { this::class.simpleName ?: "desconegut" }}"
        }
    }

    private fun Throwable.debugMessage(): String {
        val chain = generateSequence(this) { it.cause }
            .take(5)
            .mapNotNull { throwable ->
                val name = throwable::class.simpleName
                val msg = throwable.message?.trim().orEmpty()
                when {
                    msg.isNotEmpty() && !name.isNullOrBlank() -> "$name: $msg"
                    msg.isNotEmpty() -> msg
                    !name.isNullOrBlank() -> name
                    else -> null
                }
            }
            .distinct()
            .toList()

        return chain.joinToString(" | ")
    }
}
