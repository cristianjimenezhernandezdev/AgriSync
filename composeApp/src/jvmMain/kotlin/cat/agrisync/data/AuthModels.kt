package cat.agrisync.data

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val user: AuthUser
)

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUser
)

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(val session: Session) : AuthState
    data class Error(val message: String) : AuthState
}

