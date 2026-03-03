package cat.agrisync.data

class JvmEnvConfig : EnvConfig {

    companion object {
        // Defaults per desenvolupament — override via ENV o -D si cal
        private const val DEFAULT_URL = "https://bdorpgfggwmiuabqfbrh.supabase.co"
        private const val DEFAULT_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkb3JwZ2ZnZ3dtaXVhYnFmYnJoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzEzMzkwMzUsImV4cCI6MjA4NjkxNTAzNX0.vYT8fNLRgYw4OvAgUuRtq27xQkuNZPWTaBbk-tJdH9c"
    }

    private fun resolve(envName: String, default: String): String {
        System.getProperty(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return default
    }

    override fun load(): SupabaseConfig {
        return SupabaseConfig(
            url = resolve("SUPABASE_URL", DEFAULT_URL),
            anonKey = resolve("SUPABASE_ANON_KEY", DEFAULT_KEY)
        )
    }

    override fun missingMessage(): String = "Configuracio carregada amb defaults"
}
