package cat.agrisync.data

class JvmEnvConfig : EnvConfig {

    companion object {
        private const val DEFAULT_URL = "https://bdorpgfggwmiuabqfbrh.supabase.co"
        private const val DEFAULT_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkb3JwZ2ZnZ3dtaXVhYnFmYnJoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzEzMzkwMzUsImV4cCI6MjA4NjkxNTAzNX0.vYT8fNLRgYw4OvAgUuRtq27xQkuNZPWTaBbk-tJdH9c"
        private const val DEFAULT_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJkb3JwZ2ZnZ3dtaXVhYnFmYnJoIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3MTMzOTAzNSwiZXhwIjoyMDg2OTE1MDM1fQ.WqZXfg_jwObk8BMCYiEwy8hGJuy9dl1ens0_Xw2TxRU"
    }

    private fun resolve(envName: String, default: String): String {
        System.getProperty(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return default
    }

    override fun load(): SupabaseConfig {
        return SupabaseConfig(
            url = resolve("SUPABASE_URL", DEFAULT_URL),
            anonKey = resolve("SUPABASE_ANON_KEY", DEFAULT_KEY),
            serviceRoleKey = resolve("SUPABASE_SERVICE_ROLE_KEY", DEFAULT_SERVICE_KEY)
        )
    }

    override fun missingMessage(): String = "Configuracio carregada amb defaults"
}
