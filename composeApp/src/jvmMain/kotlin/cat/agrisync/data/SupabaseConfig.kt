package cat.agrisync.data

object SupabaseConfig {
    private const val EnvUrl = "SUPABASE_URL"
    private const val EnvAnonKey = "SUPABASE_ANON_KEY"

    data class Config(val url: String, val anonKey: String)

    fun fromEnv(): Config? {
        val url = System.getenv(EnvUrl)?.trim().orEmpty()
        val anonKey = System.getenv(EnvAnonKey)?.trim().orEmpty()
        if (url.isBlank() || anonKey.isBlank()) return null
        return Config(url, anonKey)
    }

    fun missingMessage(): String =
        "Falten variables d'entorn: $EnvUrl i/o $EnvAnonKey"
}

