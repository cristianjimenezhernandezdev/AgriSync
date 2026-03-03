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
    val user: AuthUser,
    val expiresAtEpochSeconds: Long? = null
)

@Serializable
data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val userEmail: String? = null,
    val expiresAtEpochSeconds: Long? = null
) {
    fun toSession(): Session = Session(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = AuthUser(id = userId, email = userEmail),
        expiresAtEpochSeconds = expiresAtEpochSeconds
    )
}

fun Session.toStored(): StoredSession = StoredSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = user.id,
    userEmail = user.email,
    expiresAtEpochSeconds = expiresAtEpochSeconds
)

@Serializable
data class TecnicDto(
    val id: String,
    val oficina_id: String,
    val user_id: String? = null,
    val nom: String,
    val email: String? = null,
    val rol: String? = null,
    val actiu: Boolean = true
)

sealed interface AuthState {
    data object Initializing : AuthState
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(val session: Session, val tecnic: TecnicDto) : AuthState
    data class Error(val message: String) : AuthState
}
