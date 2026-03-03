package cat.agrisync.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Clock

class AuthService(private val api: SupabaseAuthApi) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<AuthState>(AuthState.Initializing)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private var refreshJob: kotlinx.coroutines.Job? = null

    suspend fun initialize() {
        val stored = SessionPersistence.load() ?: run {
            _state.value = AuthState.Unauthenticated
            return
        }

        try {
            val refreshed = refreshSession(stored.refreshToken)
            val tecnic = api.getMyTecnic(refreshed.accessToken)
                ?: throw IllegalStateException("No s'ha trobat perfil tecnic")

            if (!tecnic.actiu) {
                signOut()
                _state.value = AuthState.Error("Tecnic inactiu. Contacta l'administrador.")
                return
            }

            persist(refreshed)
            _state.value = AuthState.Authenticated(refreshed, tecnic)
            scheduleRefresh(refreshed)
        } catch (_: Exception) {
            SessionPersistence.clear()
            _state.value = AuthState.Unauthenticated
        }
    }

    suspend fun login(email: String, password: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Email i password obligatoris")
        }

        _state.value = AuthState.Loading
        println("[AUTH] Cridant signInWithPassword...")

        val authResponse = api.signInWithPassword(cleanEmail, password)
        println("[AUTH] Token rebut. user_id=${authResponse.user.id}")

        val session = authResponse.toSession()
        println("[AUTH] Cridant getMyTecnic...")

        val tecnic = api.getMyTecnic(session.accessToken)
        println("[AUTH] Tecnic rebut: ${tecnic?.nom ?: "NULL"}")

        if (tecnic == null) {
            throw IllegalStateException(
                "No s'ha trobat perfil tecnic per user_id=${authResponse.user.id}. " +
                "Verifica que existeixi a public.tecnic amb user_id correcte."
            )
        }

        if (!tecnic.actiu) {
            throw IllegalStateException("Tecnic inactiu. Contacta l'administrador.")
        }

        persist(session)
        _state.value = AuthState.Authenticated(session, tecnic)
        println("[AUTH] Estat canviat a Authenticated. Tecnic=${tecnic.nom}, rol=${tecnic.rol}")
        scheduleRefresh(session)
    }

    suspend fun refreshNow(): Session {
        val current = sessionOrNull() ?: throw IllegalStateException("No hi ha sessio")
        val refreshed = refreshSession(current.refreshToken)
        val tecnic = api.getMyTecnic(refreshed.accessToken)
            ?: throw IllegalStateException("No s'ha trobat perfil tecnic")
        persist(refreshed)
        _state.value = AuthState.Authenticated(refreshed, tecnic)
        scheduleRefresh(refreshed)
        return refreshed
    }

    fun signOut() {
        refreshJob?.cancel()
        refreshJob = null
        SessionPersistence.clear()
        _state.value = AuthState.Unauthenticated
    }

    fun sessionOrNull(): Session? {
        return (state.value as? AuthState.Authenticated)?.session
    }

    fun clear() {
        scope.cancel()
    }

    private suspend fun refreshSession(refreshToken: String): Session {
        val response = api.refresh(refreshToken)
        return response.toSession()
    }

    private fun AuthResponse.toSession(): Session {
        val now = Clock.System.now().epochSeconds
        val expiresAt = expires_in?.let { now + max(30L, it) }
        return Session(
            accessToken = access_token,
            refreshToken = refresh_token,
            user = user,
            expiresAtEpochSeconds = expiresAt
        )
    }

    private fun persist(session: Session) {
        SessionPersistence.save(session.toStored())
    }

    private fun scheduleRefresh(session: Session) {
        refreshJob?.cancel()
        val expiresAt = session.expiresAtEpochSeconds ?: return
        refreshJob = scope.launch {
            val now = Clock.System.now().epochSeconds
            val delaySeconds = max(15L, expiresAt - now - 60L)
            delay(delaySeconds * 1000L)
            runCatching { refreshNow() }
                .onFailure {
                    SessionPersistence.clear()
                    _state.value = AuthState.Unauthenticated
                }
        }
    }
}
