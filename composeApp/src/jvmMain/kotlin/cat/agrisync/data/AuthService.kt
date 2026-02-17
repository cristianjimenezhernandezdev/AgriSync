package cat.agrisync.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthService(private val api: SupabaseAuthApi) {
    private val _state = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    suspend fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Email i password obligatoris")
            return
        }
        _state.value = AuthState.Loading
        try {
            val response = api.signInWithPassword(email.trim(), password)
            val session = Session(
                accessToken = response.access_token,
                refreshToken = response.refresh_token,
                user = response.user
            )
            _state.value = AuthState.Authenticated(session)
        } catch (ex: Exception) {
            val msg = ex.message?.ifBlank { null } ?: "Error d'autenticacio"
            _state.value = AuthState.Error(msg)
        }
    }

    fun signOut() {
        _state.value = AuthState.Unauthenticated
    }
}

