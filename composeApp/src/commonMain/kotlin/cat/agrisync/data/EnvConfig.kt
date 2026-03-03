package cat.agrisync.data

interface EnvConfig {
    fun load(): SupabaseConfig?
    fun missingMessage(): String
}
