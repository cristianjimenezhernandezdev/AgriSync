package cat.agrisync.data

object AppServices {
    private var authService: AuthService? = null

    fun authService(config: SupabaseConfig.Config): AuthService {
        val existing = authService
        if (existing != null) return existing
        val client = SupabaseHttpClient.create()
        val api = SupabaseAuthApi(client, config)
        return AuthService(api).also { authService = it }
    }
}

